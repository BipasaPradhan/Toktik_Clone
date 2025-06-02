from celery import Celery, chain, group
import os, shutil
from s3_client import S3Client
import redis
import json

app = Celery('tasks', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()
redis_client = redis.Redis(host='redis', port=6379, decode_responses=True)

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

    # Define S3 paths for processed files
    s3_converted_path = f"{user_id}/output/{video_id}/converted.mp4"
    s3_hls_path = f"{user_id}/output/{video_id}/playlist.m3u8"
    s3_thumb_path = f"{user_id}/output/{video_id}/thumb.jpg"

    # Chain tasks with user_id
    convert_task = app.signature('convert.convert_video', args=[video_id, s3_key, converted_path, user_id], queue='convert_queue')
    chunking_task = app.signature('chunking.chunk_video_to_hls', args=[converted_path, output_prefix, user_id], queue='chunking_queue')
    thumbnail_task = app.signature('thumbnail.extract_thumbnail', args=[converted_path, thumb_path, user_id], queue='thumbnail_queue')

    workflow = chain(
        convert_task,
        group(chunking_task, thumbnail_task),
    ).apply_async()
    print(f"Task chain enqueued with ID: {workflow.id}")

    # Wait for tasks to complete
    workflow.get()

    # Publish to video:processed channel
    message = {
        "video_id": str(video_id),
        "hls_playlist_url": s3_hls_path,
        "thumbnail_url": s3_thumb_path,
        "duration": None  # Add duration logic if needed
    }
    redis_client.publish("video:processed", json.dumps(message))
    print(f"Published to video:processed: {message}")

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

    output_dir = os.path.dirname(converted_path)
    if os.path.exists(output_dir) and not os.listdir(output_dir):
        shutil.rmtree(output_dir)
        print(f"Removed empty output directory {output_dir}")

    return {'status': 'success', 'video_id': video_id, 'task_id': workflow.id}