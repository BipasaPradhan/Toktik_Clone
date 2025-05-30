import ffmpeg
from celery import Celery
import os
import time
from s3_client import S3Client
from botocore.exceptions import ClientError

app = Celery('chunking', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(name='chunking.chunk_video_to_hls', queue='chunking_queue')
def chunk_video_to_hls(converted_path, output_prefix=None):
    # If output_prefix is not provided, derive it from converted_path
    if output_prefix is None:
        output_prefix = os.path.dirname(converted_path)

    # Define local path for downloaded converted.mp4
    local_converted_path = f"/app/temp/{os.path.basename(converted_path)}"
    os.makedirs(os.path.dirname(local_converted_path), exist_ok=True)

    # Download converted.mp4 from S3 with retry
    s3_key = f"processed/{os.path.basename(os.path.dirname(converted_path))}/converted.mp4"
    max_retries = 5
    for attempt in range(max_retries):
        try:
            s3_client.download_file(s3_key, local_converted_path)
            print(f"Downloaded {s3_key} to {local_converted_path}")
            break
        except ClientError as e:
            if e.response['Error']['Code'] == '404' and attempt < max_retries - 1:
                print(f"S3 key {s3_key} not found, retrying ({attempt + 1}/{max_retries})...")
                time.sleep(2)  # Wait 2 seconds before retrying
            else:
                raise e

    # Ensure output directory exists
    os.makedirs(output_prefix, exist_ok=True)

    # Generate HLS segments
    try:
        stream = ffmpeg.input(local_converted_path)
        stream = ffmpeg.output(
            stream,
            f"{output_prefix}/playlist.m3u8",
            vcodec='libx264',
            acodec='aac',
            f='hls',
            hls_time=2,
            hls_list_size=0,
            hls_segment_filename=f"{output_prefix}/segment_%03d.ts"
        )
        ffmpeg.run(stream)
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during HLS chunking: {error_msg}")

    # Upload HLS files to S3
    s3_prefix = f"processed/{os.path.basename(os.path.dirname(output_prefix))}"
    s3_client.upload_file(f"{output_prefix}/playlist.m3u8", s3_prefix, f"playlist.m3u8")
    for filename in os.listdir(output_prefix):
        if filename.startswith("segment_") and filename.endswith(".ts"):
            s3_client.upload_file(f"{output_prefix}/{filename}", s3_prefix, filename)

    # Cleanup
    for filename in os.listdir(output_prefix):
        file_path = os.path.join(output_prefix, filename)
        if os.path.isfile(file_path):
            os.remove(file_path)
    if os.path.exists(local_converted_path):
        os.remove(local_converted_path)

    return f"{output_prefix}/playlist.m3u8"