from dotenv import load_dotenv
import os

load_dotenv()

class Config:
    S3_ENDPOINT_URL = os.getenv("S3_ENDPOINT_URL") or "https://596cb572b0e782e28b4765cac07a6e12.r2.cloudflarestorage.com"
    S3_ACCESS_KEY = os.getenv("S3_ACCESS_KEY") or "ab3b50ee95365b573291ec1c3ce36f21"
    S3_SECRET_KEY = os.getenv("S3_SECRET_KEY") or "900d123036610ff4fe1f2dd8ce6a78ac2d8263c9b470f5dbf9da432577751a8b"
    S3_BUCKET = os.getenv("S3_BUCKET") or "toktikp2"
    S3_REGION = os.getenv("S3_REGION") or "auto"

    IS_DOCKER = os.getenv("DOCKER_ENV") == "true"
    REDIS_HOST = "redis" if IS_DOCKER else "localhost"
    CELERY_BROKER_URL = os.getenv("CELERY_BROKER_URL", f"redis://{REDIS_HOST}:6379/0")
    CELERY_RESULT_BACKEND = os.getenv("CELERY_RESULT_BACKEND", f"redis://{REDIS_HOST}:6379/0")