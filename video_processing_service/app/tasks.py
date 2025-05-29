from celery import Celery
from celery.signals import worker_process_init
from app.s3_client import S3Client
from app.video_processor import VideoProcessor
import os
import shutil

app = Celery('tasks')

@worker_process_init.connect
def configure_worker(**kwargs):
    app.conf.update(
        broker_url='redis://localhost:6379/0',
        result_backend='redis://localhost:6379/0',
        task_serializer='json',
        result_serializer='json',
        accept_content=['json'],
        timezone='UTC',
        enable_utc=True,
        broker_connection_retry_on_startup=True
    )
    print(f"Worker init broker_url: {app.conf.broker_url}")

print(f"Initial broker_url: {app.conf.broker_url}")

@app.task
def process_video_task(video_id: str, s3_key: str):
    try:
        s3_client = S3Client()
        processor = VideoProcessor()
        local_path = f"uploads/{video_id}.mp4"
        output_prefix = f"output/{video_id}"
        os.makedirs(output_prefix, exist_ok=True)

        # Download
        s3_client.download_file(s3_key, local_path)

        # Process
        processor.validate_video_duration(local_path)
        converted = processor.convert_video(local_path, f"{output_prefix}/converted.mp4")
        playlist, segments = processor.chunk_video_to_hls(converted, video_id)
        thumbnail = processor.extract_thumbnail(converted, f"{output_prefix}/thumb.jpg")

        # Upload
        for segment in segments:
            s3_client.upload_file(f"output/{video_id}/{segment}", f"processed/{video_id}/{segment}")
        s3_client.upload_file(playlist, f"processed/{video_id}/playlist.m3u8")
        s3_client.upload_file(thumbnail, f"thumbnails/{video_id}.jpg")

        # Cleanup
        os.remove(local_path)
        shutil.rmtree(f"output/{video_id}")

        return {"status": "success", "video_id": video_id, "s3_key": s3_key}
    except Exception as e:
        return {"status": "failed", "video_id": video_id, "message": str(e)}