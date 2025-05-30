import ffmpeg
from celery import Celery
import os

app = Celery('convert', broker='redis://redis:6379/0', backend='redis://redis:6379/0')

@app.task
def convert_video(input_path, output_path):
    try:
        stream = ffmpeg.input(input_path)
        stream = ffmpeg.output(stream, output_path, vcodec='libx264', preset='medium')
        ffmpeg.run(stream)
        return output_path
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during conversion: {error_msg}")