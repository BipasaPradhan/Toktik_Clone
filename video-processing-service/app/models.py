from pydantic import BaseModel

class VideoProcessRequest(BaseModel):
    video_id: str
    s3_key: str