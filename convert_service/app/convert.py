import ffmpeg
from celery import Celery
import os
import shutil
from .s3_client import S3Client
from botocore.exceptions import ClientError

app = Celery('convert', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(
    name='convert.convert_video',
    queue='convert_queue',
    autoretry_for=(
        ClientError, 
        ffmpeg.Error,  
        OSError,      
    ),
    retry_kwargs={'max_retries': 3, 'countdown': 30},
    retry_backoff=True,
    retry_jitter=True
)
def convert_video(video_id, s3_key, converted_key, user_id):
    # temporary local paths within the pod's ephemeral storage
    temp_dir = f"/tmp/video_convert/{video_id}"
    input_path = f"{temp_dir}/input.mp4"
    output_path = f"{temp_dir}/converted.mp4"

    # Ensure temporary directories exist
    os.makedirs(temp_dir, exist_ok=True)

    # Download the video from R2
    print(f"Downloading video from R2: {s3_key}")
    s3_client.download_file(s3_key, input_path)

    # Convert the video
    try:
        stream = ffmpeg.input(input_path)
        stream = ffmpeg.output(stream, output_path, vcodec='libx264', preset='medium')
        ffmpeg.run(stream)
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during conversion: {error_msg}")

    # Upload the converted video to R2
    print(f"Uploading converted video to R2: {converted_key}")
    s3_client.upload_file(output_path, converted_key)

    # Cleanup temporary files
    for file_path in [input_path, output_path]:
        if os.path.exists(file_path):
            try:
                os.remove(file_path)
                print(f"Removed temporary file: {file_path}")
            except Exception as e:
                print(f"Failed to remove temporary file {file_path}: {e}")

    # Remove the temporary directory if empty
    if os.path.exists(temp_dir) and not os.listdir(temp_dir):
        shutil.rmtree(temp_dir)
        print(f"Removed empty temporary directory: {temp_dir}")

    print(f"Video conversion completed successfully for video_id: {video_id}. Returning converted_key: {converted_key}")
    return converted_key  # Return the R2 key for the converted video