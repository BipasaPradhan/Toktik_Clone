from fastapi import FastAPI
from app.config import Config

app = FastAPI()

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "s3_endpoint": Config.S3_ENDPOINT_URL,
        "s3_bucket": Config.S3_BUCKET
    }