from pydantic import BaseModel

class VideoProcessRequest(BaseModel):
    video_id: str | None = None
    s3_key: str