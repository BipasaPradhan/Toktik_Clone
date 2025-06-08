broker_url = 'redis://localhost:6379/0'
result_backend = 'redis://localhost:6379/0'
task_serializer = 'json'
result_serializer = 'json'
accept_content = ['json']
timezone = 'UTC'
enable_utc = True
broker_connection_retry_on_startup = True
task_routes = {
    'convert.convert_video': {'queue': 'convert_queue'},
    'chunking.chunk_video_to_hls': {'queue': 'chunking_queue'},
    'thumbnail.extract_thumbnail': {'queue': 'thumbnail_queue'},
    'tasks.process_video_task': {'queue': 'video_processing_queue'},
}