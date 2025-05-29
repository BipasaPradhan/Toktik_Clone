import ffmpeg
from celery import Celery
import os

app = Celery('thumbnail', broker='redis://redis:6379/0', backend='redis://redis:6379/0')

@app.task
def extract_thumbnail(input_path, output_path):
    try:
        stream = ffmpeg.input(input_path, ss=1)
        stream = ffmpeg.output(stream, output_path, vframes=1, vcodec='mjpeg')
        ffmpeg.run(stream)
        return output_path
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during thumbnail extraction: {error_msg}")