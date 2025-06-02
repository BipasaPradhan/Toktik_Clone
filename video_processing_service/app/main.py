from fastapi import FastAPI
from config import Config
from models import VideoProcessRequest
from tasks import process_video_task
import logging
import os
import json
import redis
import asyncio

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

# File to store the counter
COUNTER_FILE = "/app/counter/counter.json"

# Initialize or read counter
def get_next_video_id():
    if not os.path.exists(COUNTER_FILE):
        with open(COUNTER_FILE, 'w') as f:
            json.dump({"counter": 0}, f)

    with open(COUNTER_FILE, 'r') as f:
        data = json.load(f)
        counter = data["counter"]

    counter += 1
    with open(COUNTER_FILE, 'w') as f:
        json.dump({"counter": counter}, f)

    return f"video{counter}"

# Reset counter function
def reset_counter():
    with open(COUNTER_FILE, 'w') as f:
        json.dump({"counter": 0}, f)
    logger.info("Counter reset to 0")

process_video_task._app.conf.update(
    broker_url=Config.CELERY_BROKER_URL,
    result_backend=Config.CELERY_RESULT_BACKEND
)

# Redis client
redis_client = redis.Redis(host='redis', port=6379, decode_responses=True)

async def consume_redis_messages():
    pubsub = redis_client.pubsub()
    pubsub.subscribe('video:process')
    logger.info("Subscribed to video:process channel")
    while True:
        message = pubsub.get_message()
        if message and message['type'] == 'message':
            data = message['data']
            logger.info(f"Received Redis message: {data}")
            try:
                # Parse the message (assuming it's a JSON string)
                import json
                message_data = json.loads(data)
                video_id = message_data.get('video_id')
                s3_key = message_data.get('s3_key')
                if video_id and s3_key:
                    logger.info(f"Triggering process_video_task with video_id={video_id}, s3_key={s3_key}")
                    process_video_task.apply_async(args=[video_id, s3_key], queue='video_processing_queue')
                else:
                    logger.warning("Invalid message format, missing video_id or s3_key")
            except json.JSONDecodeError as e:
                logger.error(f"Failed to parse Redis message: {e}")
        await asyncio.sleep(0.1)  # Avoid busy loop

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(consume_redis_messages())

@app.get("/health")
async def health_check():
    logger.info("Health check accessed")
    return {
        "status": "healthy",
        "s3_endpoint": Config.S3_ENDPOINT_URL,
        "s3_bucket": Config.S3_BUCKET
    }

@app.post("/reset-counter")
async def reset_counter_endpoint():
    reset_counter()
    return {"message": "Counter reset successfully"}

@app.post("/process-video")
async def process_video(request: VideoProcessRequest):
    try:
        # Use provided video_id or generate a new one
        video_id = request.video_id if request.video_id else get_next_video_id()
        logger.info(f"Received request: video_id={video_id}, s3_key={request.s3_key}")
        task = process_video_task.apply_async(args=[video_id, request.s3_key], queue='video_processing_queue')
        logger.info(f"Task enqueued: task_id={task.id}")
        return {
            "message": "Video processing task enqueued",
            "task_id": task.id,
            "video_id": video_id,
            "s3_key": request.s3_key
        }
    except Exception as e:
        logger.error(f"Error processing video: {str(e)}")
        raise