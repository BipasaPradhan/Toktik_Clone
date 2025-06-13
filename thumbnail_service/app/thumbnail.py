import ffmpeg
from celery import Celery
import os
import shutil
from .s3_client import S3Client
from botocore.exceptions import ClientError

app = Celery('thumbnail', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(
    name='thumbnail.extract_thumbnail', 
    queue='thumbnail_queue',
    autoretry_for=(
        ClientError,  
        ffmpeg.Error,  
        OSError,      
    ),
    retry_kwargs={'max_retries': 3, 'countdown': 30},
    retry_backoff=True,
    retry_jitter=True
)
def extract_thumbnail(converted_key, user_id, thumbnail_key):
    video_id = os.path.basename(os.path.dirname(thumbnail_key))
    print(f"Starting thumbnail extraction for video_id: {video_id}, user_id: {user_id}")

    # Create temp directories
    temp_dir = f"/tmp/video_thumb/{video_id}"
    input_path = f"{temp_dir}/input.mp4"
    output_path = f"{temp_dir}/thumb.jpg"

    os.makedirs(temp_dir, exist_ok=True)
    print(f"Created temporary directory: {temp_dir}")

    # Download converted.mp4 from R2
    print(f"Downloading converted video from R2: {converted_key}")
    try:
        s3_client.download_file(converted_key, input_path)
        print(f"Downloaded {converted_key} to {input_path}")
    except ClientError as e:
        raise Exception(f"Error downloading converted.mp4 from R2: {e}")

    try:
        stream = ffmpeg.input(input_path, ss=1)
        stream = ffmpeg.output(stream, output_path, vframes=1, format='image2', vcodec='mjpeg')
        ffmpeg.run(stream)
        print(f"Thumbnail extracted to {output_path}")
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during thumbnail extraction: {error_msg}")

    # Upload to R2
    print(f"Uploading thumbnail to R2: {thumbnail_key}")
    try:
        s3_client.upload_file(output_path, "toktikp2", thumbnail_key)
    except ClientError as e:
        raise Exception(f"Error uploading thumbnail to R2: {e}")
    print(f"Uploaded thumbnail to R2: {thumbnail_key}")

    # Cleanup
    for file_path in [input_path, output_path]:
        if os.path.exists(file_path):
            try:
                os.remove(file_path)
                print(f"Removed temporary file: {file_path}")
            except Exception as e:
                print(f"Failed to remove temporary file {file_path}: {e}")

    # Remove the empty temporary directory
    if os.path.exists(temp_dir) and not os.listdir(temp_dir):
        shutil.rmtree(temp_dir)
        print(f"Removed empty temporary directory {temp_dir}")

    print(f"Thumbnail extraction completed for video_id: {video_id}")
    return thumbnail_key