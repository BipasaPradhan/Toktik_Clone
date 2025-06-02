import ffmpeg
from celery import Celery
import os
import shutil
from .s3_client import S3Client

app = Celery('convert', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(name='convert.convert_video')
def convert_video(video_id, s3_key, output_path, user_id):
    # Download the video from S3
    input_path = f"/app/uploads/{video_id}.mp4"
    os.makedirs(os.path.dirname(input_path), exist_ok=True)
    s3_client.download_file(s3_key, input_path)

    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    # Convert the video
    try:
        stream = ffmpeg.input(input_path)
        stream = ffmpeg.output(stream, output_path, vcodec='libx264', preset='medium')
        ffmpeg.run(stream)
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during conversion: {error_msg}")

    # Upload the converted video to S3
    s3_key = f"{user_id}/output/{video_id}/converted.mp4"
    s3_client.upload_file(output_path, "toktikp2", s3_key)

    # Cleanup
    if os.path.exists(input_path):
        os.remove(input_path)
    if os.path.exists(output_path):
        os.remove(output_path)

    # Remove the empty output directory
    output_dir = os.path.dirname(output_path)
    if os.path.exists(output_dir) and not os.listdir(output_dir):
        shutil.rmtree(output_dir)
        print(f"Removed empty output directory {output_dir}")

    return output_path