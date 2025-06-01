import ffmpeg
from celery import Celery
import os
from .s3_client import S3Client
from botocore.exceptions import ClientError

app = Celery('thumbnail', broker='redis://redis:6379/0', backend='redis://redis:6379/0')
s3_client = S3Client()

@app.task(name='thumbnail.extract_thumbnail', queue='thumbnail_queue')
def extract_thumbnail(converted_path, thumb_path):
    os.makedirs(os.path.dirname(thumb_path), exist_ok=True)

    try:
        stream = ffmpeg.input(converted_path, ss=1)
        stream = ffmpeg.output(stream, thumb_path, vframes=1, vcodec='mjpeg')
        ffmpeg.run(stream)
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during thumbnail extraction: {error_msg}")

    video_id = os.path.basename(os.path.dirname(converted_path))
    s3_key = f"output/{video_id}/thumb.jpg"
    s3_client.upload_file(thumb_path, "toktikp2", s3_key)

    # Cleanup
    # if os.path.exists(thumb_path):
    #     os.remove(thumb_path)

    return thumb_path