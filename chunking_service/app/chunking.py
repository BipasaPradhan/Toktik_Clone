import ffmpeg
from celery import Celery
import os
import time
import shutil
from s3_client import S3Client
from botocore.exceptions import ClientError

app = Celery('chunking', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(
    name='chunking.chunk_video_to_hls',
    queue='chunking_queue',
    autoretry_for=(
            ClientError,
            ffmpeg.Error,
            OSError,
    ),
    retry_kwargs={'max_retries': 3, 'countdown': 30},
    retry_backoff=True,
    retry_jitter=True
)
def chunk_video_to_hls(convert_result, user_id, hls_playlist_key):
    video_id = os.path.basename(os.path.dirname(hls_playlist_key))
    converted_key = convert_result["converted_key"]
    duration = convert_result["duration"]
    print(f"Starting chunking for video_id: {video_id}, user_id: {user_id}, duration: {duration}")

    temp_dir = f"/tmp/video_chunk/{video_id}"
    input_path = f"{temp_dir}/input.mp4"
    output_dir = f"{temp_dir}/output"
    playlist_path = f"{output_dir}/playlist.m3u8"

    os.makedirs(temp_dir, exist_ok=True)
    os.makedirs(output_dir, exist_ok=True)
    print(f"Created temporary directories: {temp_dir}, {output_dir}")

    print(f"Downloading converted video from R2: {converted_key}")
    max_retries = 5
    for attempt in range(max_retries):
        try:
            s3_client.download_file(converted_key, input_path)
            print(f"Downloaded {converted_key} to {input_path}")
            break
        except ClientError as e:
            if e.response['Error']['Code'] == '404' and attempt < max_retries - 1:
                print(f"S3 key {converted_key} not found, retrying ({attempt + 1}/{max_retries})...")
                time.sleep(2)
            else:
                raise e
    print(f"Successfully downloaded converted video to {input_path}")

    try:
        stream = ffmpeg.input(input_path)
        stream = ffmpeg.output(
            stream,
            playlist_path,
            vcodec='libx264',
            acodec='aac',
            f='hls',
            hls_time=2,
            hls_list_size=0,
            hls_segment_filename=f"{output_dir}/segment_%03d.ts"
        )
        ffmpeg.run(stream)
        print(f"Generated HLS playlist: {playlist_path}")
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode('utf-8', errors='ignore') if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during HLS chunking: {error_msg}")

    print(f"Uploading HLS playlist to R2: {hls_playlist_key}")
    try:
        s3_client.upload_file(playlist_path, "toktikp2", hls_playlist_key)
        print(f"Uploaded HLS playlist to R2: {hls_playlist_key}")
    except Exception as e:
        print(f"Failed to upload HLS playlist to R2: {hls_playlist_key}: {e}")

    for filename in os.listdir(output_dir):
        if filename.startswith("segment_") and filename.endswith(".ts"):
            file_path = os.path.join(output_dir, filename)
            segment_key = f"{os.path.dirname(hls_playlist_key)}/{filename}"
            print(f"Uploading segment to R2: {segment_key}")
            try:
                s3_client.upload_file(file_path, "toktikp2", segment_key)
                print(f"Uploaded segment {filename} to R2: {segment_key}")
            except Exception as e:
                print(f"Failed to upload segment {filename} to R2: {segment_key}: {e}")

    for filename in os.listdir(output_dir):
        file_path = os.path.join(output_dir, filename)
        if os.path.isfile(file_path):
            try:
                os.remove(file_path)
                print(f"Removed temporary file: {file_path}")
            except Exception as e:
                print(f"Failed to remove temporary file {file_path}: {e}")
    if os.path.exists(input_path):
        try:
            os.remove(input_path)
            print(f"Removed temporary input file: {input_path}")
        except Exception as e:
            print(f"Failed to remove temporary file {input_path}: {e}")

    if os.path.exists(output_dir) and not os.listdir(output_dir):
        shutil.rmtree(output_dir)
        print(f"Removed empty output directory {output_dir}")

    print(f"Chunking completed for video_id: {video_id}")
    return {"hls_playlist_key": hls_playlist_key, "duration": duration}