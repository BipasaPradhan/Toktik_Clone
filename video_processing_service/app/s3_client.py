import boto3
from botocore.client import Config as BotoConfig
from app.config import Config

class S3Client:
    def __init__(self):
        self.s3 = boto3.client(
            's3',
            endpoint_url=Config.S3_ENDPOINT_URL,
            aws_access_key_id=Config.S3_ACCESS_KEY,
            aws_secret_access_key=Config.S3_SECRET_KEY,
            region_name=Config.S3_REGION,
            config=BotoConfig(signature_version='s3v4')
        )
        self.bucket = Config.S3_BUCKET

    def download_file(self, s3_key, local_path):
        try:
            self.s3.download_file(self.bucket, s3_key, local_path)
        except Exception as e:
            raise Exception(f"S3 download failed: {str(e)}")

    def upload_file(self, file_path, s3_key):
        try:
            self.s3.upload_file(file_path, self.bucket, s3_key)
        except Exception as e:
            raise Exception(f"S3 upload failed: {str(e)}")

    def generate_presigned_url(self, s3_key, expiration=3600):
        try:
            return self.s3.generate_presigned_url(
                'get_object',
                Params={'Bucket': self.bucket, 'Key': s3_key},
                ExpiresIn=expiration
            )
        except Exception as e:
            raise Exception(f"Presigned URL generation failed: {str(e)}")