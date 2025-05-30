import boto3
import os
import botocore

class S3Client:
    def __init__(self):
        self.s3_client = boto3.client(
            's3',
            endpoint_url=os.getenv('S3_ENDPOINT_URL'),
            aws_access_key_id=os.getenv('S3_ACCESS_KEY'),
            aws_secret_access_key=os.getenv('S3_SECRET_KEY'),
            region_name=os.getenv('S3_REGION')
        )
        self.bucket = os.getenv('S3_BUCKET')

    def download_file(self, s3_key, local_path):
        try:
            os.makedirs(os.path.dirname(local_path), exist_ok=True)
            self.s3_client.download_file(self.bucket, s3_key, local_path)
        except botocore.exceptions.ClientError as e:
            raise e

    def upload_file(self, local_path, bucket, s3_key):
        try:
            # Ignore the 'bucket' parameter since it's already set as self.bucket
            self.s3_client.upload_file(local_path, self.bucket, s3_key)
        except botocore.exceptions.ClientError as e:
            raise e