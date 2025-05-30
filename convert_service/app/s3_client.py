import os
import boto3
from botocore.exceptions import ClientError

class S3Client:
    def __init__(self):
        self.s3 = boto3.client('s3',
                               endpoint_url=os.getenv('S3_ENDPOINT_URL'),
                               aws_access_key_id=os.getenv('S3_ACCESS_KEY'),
                               aws_secret_access_key=os.getenv('S3_SECRET_KEY'),
                               region_name=os.getenv('S3_REGION'))

    def upload_file(self, file_path, bucket_key):
        try:
            self.s3.upload_file(file_path, os.getenv('S3_BUCKET'), bucket_key)
        except ClientError as e:
            raise Exception(f"Error uploading to S3: {e}")

    def download_file(self, bucket_key, file_path):
        try:
            self.s3.download_file(os.getenv('S3_BUCKET'), bucket_key, file_path)
        except ClientError as e:
            raise Exception(f"Error downloading from S3: {e}")