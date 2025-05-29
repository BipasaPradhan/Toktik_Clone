import ffmpeg
from celery import Celery
import os

app = Celery('chunking', broker='redis://redis:6379/0', backend='redis://redis:6379/0')

@app.task
def chunk_video(input_path, output_prefix):
    try:
        stream = ffmpeg.input(input_path)
        stream = ffmpeg.output(stream, f"{output_prefix}/playlist.m3u8", vcodec='libx264', hls_time=2, hls_list_size=0)
        ffmpeg.run(stream)
        return f"{output_prefix}/playlist.m3u8"
    except ffmpeg.Error as e:
        error_msg = e.stderr.decode() if e.stderr else "No stderr output from FFmpeg"
        raise Exception(f"FFmpeg error during chunking: {error_msg}")