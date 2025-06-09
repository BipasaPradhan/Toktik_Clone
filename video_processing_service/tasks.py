from celery import Celery, chain, group
import os
import shutil
import redis
import json
from s3_client import S3Client

app = Celery('tasks', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
app.config_from_object('celeryconfig')
s3_client = S3Client()
redis_client = redis.Redis(host='redis', port=6379, decode_responses=True)

@app.task(queue='video_processing_queue')
def process_video_task(video_id, s3_key, user_id):
    print(f"Enqueuing process_video_task for video_id: {video_id}")

    # Extract userId from s3_key
    print(f"Extracted userId: {user_id}")

    # Define S3 output keys
    converted_key = f"{user_id}/output/{video_id}/converted.mp4"
    hls_playlist_key = f"{user_id}/output/{video_id}/playlist.m3u8"
    thumbnail_key = f"{user_id}/output/{video_id}/thumb.jpg"

    # Chain tasks
    convert_task = app.signature('convert.convert_video', args=[video_id, s3_key, converted_key, user_id], queue = 'convert_queue')
    chunking_task = app.signature('chunking.chunk_video_to_hls', args=[user_id, hls_playlist_key], queue='chunking_queue')
    thumbnail_task = app.signature('thumbnail.extract_thumbnail', args=[user_id, thumbnail_key], queue='thumbnail_queue')
    update_metadata_task = app.signature('tasks.update_metadata', args=[video_id, converted_key], queue='video_processing_queue')

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

    return {'status': 'success', 'video_id': video_id, 'task_id': workflow.id}

@app.task(queue='video_processing_queue')
def update_metadata(group_result, converted_key):
    print(f"Updating metadata for video_id: {video_id}")

    # Extract hls_playlist_key and thumbnail_key from group_result
    try:
        hls_playlist_key, thumbnail_key = group_result
    except (TypeError, ValueError) as e:
        print(f"Error unpacking group_result: {e}")
        raise Exception(f"Failed to unpack group_result: {e}")

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

    return {'status': 'success', 'video_id': video_id}