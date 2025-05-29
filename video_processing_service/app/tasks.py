from celery import Celery
import os
from app.s3_client import S3Client

app = Celery('tasks', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task
def process_video_task(video_id, s3_key):
    # Download from S3
    local_path = f"/app/uploads/{video_id}.mp4"
    s3_client.download_file(s3_key, local_path)

    # Convert
    converted_path = f"/app/output/{video_id}/converted.mp4"
    os.makedirs(os.path.dirname(converted_path), exist_ok=True)
    from app.convert import convert_video
    converted_path = convert_video.delay(local_path, converted_path).get()

    # Chunk
    output_prefix = f"/app/output/{video_id}"
    from app.chunking import chunk_video
    playlist_path = chunk_video.delay(converted_path, output_prefix).get()

    # Extract Thumbnail
    thumb_path = f"/app/output/{video_id}/thumb.jpg"
    from app.thumbnail import extract_thumbnail
    thumb_path = extract_thumbnail.delay(converted_path, thumb_path).get()

    # Upload to S3
    s3_client.upload_file(playlist_path, f"processed/{video_id}/playlist.m3u8")
    s3_client.upload_file(thumb_path, f"thumbnails/{video_id}/thumb.jpg")

    # Cleanup
    files_to_remove = [
        local_path,
        converted_path,
        playlist_path,
        thumb_path
    ]
    for file_path in files_to_remove:
        if os.path.exists(file_path):
            os.remove(file_path)

    return {'status': 'success', 'video_id': video_id, 's3_key': s3_key}