from celery import Celery, chain, group
import os, shutil
from s3_client import S3Client

app = Celery('tasks', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(queue='video_processing_queue')
def process_video_task(video_id, s3_key):
    print(f"Enqueuing process_video_task for video_id: {video_id}")

    # Extract userId from s3_key
    user_id = s3_key.split('/')[0]
    print(f"Extracted userId: {user_id}")

    # Download from S3
    local_path = f"/app/uploads/{video_id}.mp4"
    s3_client.download_file(s3_key, local_path)
    print(f"Downloaded video to {local_path}")

    # Define output paths
    converted_path = f"/app/output/{video_id}/converted.mp4"
    output_prefix = f"/app/output/{video_id}"
    thumb_path = f"/app/output/{video_id}/thumb.jpg"

    os.makedirs(os.path.dirname(converted_path), exist_ok=True)

    # Chain tasks with explicit arguments
    print("Sending task to convert.convert_video")
    convert_task = app.signature('convert.convert_video', args=[video_id, s3_key, converted_path], queue='convert_queue')
    chunking_task = app.signature('chunking.chunk_video_to_hls', args=[converted_path, output_prefix], queue='chunking_queue')
    thumbnail_task = app.signature('thumbnail.extract_thumbnail', args=[converted_path, thumb_path], queue='thumbnail_queue')
    # db_task = app.signature('video_service.update_urls', args=[s3_key, converted_url, hls_url], queue='video_service_queue')

    # Use a group to run chunking and thumbnail in parallel after conversion
    workflow = chain(
        convert_task,
        group(chunking_task, thumbnail_task),
        # db_task
    ).apply_async()
    print(f"Task chain enqueued with ID: {workflow.id}")

    # Cleanup
    files_to_remove = [local_path]
    for file_path in files_to_remove:
        if os.path.exists(file_path):
            print(f"Attempting to remove {file_path}")
            try:
                os.remove(file_path)
                print(f"Successfully removed {file_path}")
            except Exception as e:
                print(f"Failed to remove {file_path}: {e}")
        else:
            print(f"File {file_path} does not exist for cleanup")

    # Cleanup the empty output directory
    output_dir = os.path.dirname(converted_path)
    if os.path.exists(output_dir) and not os.listdir(output_dir):  # Check if directory is empty
        shutil.rmtree(output_dir)
        print(f"Removed empty output directory {output_dir}")

    return {'status': 'success', 'video_id': video_id, 'task_id': workflow.id}