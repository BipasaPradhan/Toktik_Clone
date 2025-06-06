from celery import Celery, chain, group
import os
import shutil
import redis
import json
from s3_client import S3Client

app = Celery('tasks', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()
redis_client = redis.Redis(host='redis', port=6379, decode_responses=True)

@app.task(queue='video_processing_queue')
def process_video_task(video_id, s3_key, user_id):
    print(f"Enqueuing process_video_task for video_id: {video_id}")

    # Extract userId from s3_key
    # user_id = s3_key.split('/')[0]
    print(f"Extracted userId: {user_id}")

    # Download from S3
    local_path = f"/app/uploads/{video_id}.mp4"
    os.makedirs("/app/uploads", exist_ok=True)
    s3_client.download_file(s3_key, local_path)
    print(f"Downloaded video to {local_path}")

    # Define output paths
    converted_path = f"/app/output/{video_id}/converted.mp4"
    output_prefix = f"/app/output/{video_id}"
    thumb_path = f"/app/output/{video_id}/thumb.jpg"
    hls_playlist_path = f"{output_prefix}/playlist.m3u8"

    os.makedirs(os.path.dirname(converted_path), exist_ok=True)

    # Define S3 output keys
    hls_playlist_key = f"{user_id}/output/{video_id}/playlist.m3u8"
    thumbnail_key = f"{user_id}/output/{video_id}/thumb.jpg"
    converted_key = f"{user_id}/output/{video_id}/converted.mp4"

    # Chain tasks
    convert_task = app.signature('convert.convert_video', args=[video_id, s3_key, converted_path, user_id], queue='convert_queue')
    chunking_task = app.signature('chunking.chunk_video_to_hls', args=[output_prefix, user_id], queue='chunking_queue')
    thumbnail_task = app.signature('thumbnail.extract_thumbnail', args=[thumb_path, user_id], queue='thumbnail_queue')
    update_metadata_task = app.signature('tasks.update_metadata', args=[video_id, hls_playlist_key, thumbnail_key, converted_key], queue='video_processing_queue')

    # Run chunking and thumbnail in parallel, followed by metadata update
    workflow = chain(
        convert_task,
        group(
            chunking_task,
            thumbnail_task
        ),
        update_metadata_task
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
    if os.path.exists(output_dir) and not os.listdir(output_dir):
        shutil.rmtree(output_dir)
        print(f"Removed empty output directory {output_dir}")

    return {'status': 'success', 'video_id': video_id, 'task_id': workflow.id}

@app.task(queue='video_processing_queue')
def update_metadata(group_result, video_id, hls_playlist_key, thumbnail_key, converted_key):
    print(f"Updating metadata for video_id: {video_id}")
    try:
        hls_playlist_path, thumb_path = group_result
    except (TypeError, ValueError) as e:
        print(f"Error unpacking group_result: {e}")
        hls_playlist_path = f"/app/output/{video_id}/playlist.m3u8"
        thumb_path = f"/app/output/{video_id}/thumb.jpg"

    # Upload HLS playlist and thumbnail to S3
    if os.path.exists(hls_playlist_path):
        s3_client.upload_file(hls_playlist_path, hls_playlist_key)
        print(f"Uploaded HLS playlist to S3: {hls_playlist_key}")
    else:
        print(f"HLS playlist not found: {hls_playlist_path}")

    if os.path.exists(thumb_path):
        s3_client.upload_file(thumb_path, thumbnail_key)
        print(f"Uploaded thumbnail to S3: {thumbnail_key}")
    else:
        print(f"Thumbnail not found: {thumb_path}")

    # Publish to video:processed (keep for backward compatibility)
    message_redis = {
        "video_id": video_id,
        "hls_playlist_url": hls_playlist_key,
        "thumbnail_url": thumbnail_key,
        "converted_url": converted_key,
        "duration": None
    }
    redis_client.publish("video:processed", json.dumps(message_redis))
    print(f"Published to video:processed: {message_redis}")

    # Cleanup
    output_dir = os.path.dirname(hls_playlist_path)
    if os.path.exists(output_dir):
        shutil.rmtree(output_dir)
        print(f"Cleaned up output directory: {output_dir}")

    return {'status': 'success', 'video_id': video_id}