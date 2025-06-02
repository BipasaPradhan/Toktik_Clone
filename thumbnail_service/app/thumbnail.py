import ffmpeg
from celery import Celery
import os
import shutil
from .s3_client import S3Client
from botocore.exceptions import ClientError

app = Celery('thumbnail', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(name='thumbnail.extract_thumbnail', queue='thumbnail_queue')
def extract_thumbnail(converted_path, thumb_path, user_id):
    # Extract video_id from converted_path
    print(f"Received arguments - converted_path: {converted_path}, thumb_path: {thumb_path}, user_id: {user_id}")
    video_id = os.path.basename(os.path.dirname(converted_path))
    s3_key_converted = f"{user_id}/output/{video_id}/converted.mp4"
    local_temp_path = f"/tmp/{video_id}_converted.mp4"
    thumb_path = f"/app/output/{video_id}/thumb.jpg"
    print(f"Overriding thumb_path to: {thumb_path}")

    # Download converted.mp4 from R2
    try:
        os.makedirs(os.path.dirname(local_temp_path), exist_ok=True)
        s3_client.download_file(s3_key_converted, local_temp_path)
        print(f"Downloaded {s3_key_converted} to {local_temp_path}")
    except ClientError as e:
        raise Exception(f"Error downloading converted.mp4 from R2: {e}")

    # Ensure output directory exists
    os.makedirs(os.path.dirname(thumb_path), exist_ok=True)

    try:
        stream = ffmpeg.input(local_temp_path, ss=1)
        stream = ffmpeg.output(stream, thumb_path, vframes=1, format='image2', vcodec='mjpeg')
        ffmpeg.run(stream)
        print(f"Thumbnail extracted to {thumb_path}")
        if os.path.exists(thumb_path):
            print(f"Verified {thumb_path} exists with size {os.path.getsize(thumb_path)} bytes")
        else:
            print(f"Error: {thumb_path} was not created")
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during thumbnail extraction: {error_msg}")

    # Upload to R2
    s3_key = f"{user_id}/output/{video_id}/thumb.jpg"
    s3_client.upload_file(thumb_path, "toktikp2", s3_key)
    print(f"Uploaded thumbnail to R2: {s3_key}")

    # Cleanup
    if os.path.exists(local_temp_path):
        os.remove(local_temp_path)
    if os.path.exists(thumb_path):
        os.remove(thumb_path)

    # Remove the empty output directory
    output_dir = os.path.dirname(thumb_path)
    if os.path.exists(output_dir) and not os.listdir(output_dir):
        shutil.rmtree(output_dir)
        print(f"Removed empty output directory {output_dir}")

    return thumb_path