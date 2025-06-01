import boto3
from botocore.exceptions import ClientError
import os

class S3Client:
    def __init__(self):
        self.s3_client = boto3.client(
            's3',
            endpoint_url=os.getenv('S3_ENDPOINT_URL'),
            aws_access_key_id=os.getenv('S3_ACCESS_KEY'),
            aws_secret_access_key=os.getenv('S3_SECRET_KEY'),
            region_name=os.getenv('S3_REGION')
        )

    def download_file(self, s3_key: str, local_path: str) -> None:
        try:
            self.s3_client.download_file('toktikp2', s3_key, local_path)
        except ClientError as e:
            raise Exception(f"Failed to download from S3: {str(e)}")

    def upload_file(self, local_path: str, bucket: str, s3_key: str) -> None:
        try:
            self.s3_client.upload_file(local_path, bucket, s3_key)
        except ClientError as e:
            raise Exception(f"Failed to upload to S3: {str(e)}")