from fastapi import FastAPI
from app.config import Config
from app.models import VideoProcessRequest
from app.tasks import process_video_task

app = FastAPI()

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "s3_endpoint": Config.S3_ENDPOINT_URL,
        "s3_bucket": Config.S3_BUCKET
    }

@app.post("/process-video")
async def process_video(request: VideoProcessRequest):
    task = process_video_task.delay(request.video_id, request.s3_key)
    return {
        "message": "Video processing task enqueued",
        "task_id": task.id,
        "video_id": request.video_id,
        "s3_key": request.s3_key
    }