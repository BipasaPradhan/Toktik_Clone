from fastapi import FastAPI
from config import Config
from models import VideoProcessRequest
from tasks import process_video_task
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

process_video_task._app.conf.update(
    broker_url=Config.CELERY_BROKER_URL,
    result_backend=Config.CELERY_RESULT_BACKEND
)

@app.get("/health")
async def health_check():
    logger.info("Health check accessed")
    return {
        "status": "healthy",
        "s3_endpoint": Config.S3_ENDPOINT_URL,
        "s3_bucket": Config.S3_BUCKET
    }

@app.post("/process-video")
async def process_video(request: VideoProcessRequest):
    try:
        logger.info(f"Received request: video_id={request.video_id}, s3_key={request.s3_key}")
        task = process_video_task.apply_async(args=[request.video_id, request.s3_key], queue='video_processing_queue')
        logger.info(f"Task enqueued: task_id={task.id}")
        return {
            "message": "Video processing task enqueued",
            "task_id": task.id,
            "video_id": request.video_id,
            "s3_key": request.s3_key
        }
    except Exception as e:
        logger.error(f"Error processing video: {str(e)}")
        raise