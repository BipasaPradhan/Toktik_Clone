import ffmpeg
import os

class VideoProcessor:
    def __init__(self, upload_dir="uploads", output_dir="output"):
        self.upload_dir = upload_dir
        self.output_dir = output_dir
        os.makedirs(upload_dir, exist_ok=True)
        os.makedirs(output_dir, exist_ok=True)

    def validate_video_duration(self, file_path):
        try:
            probe = ffmpeg.probe(file_path)
            duration = float(probe['format']['duration'])
            if duration > 60:
                raise ValueError("Video duration exceeds 1 minute")
            return True
        except ffmpeg.Error as e:
            raise Exception(f"FFmpeg error during validation: {e.stderr.decode()}")

    def convert_video(self, input_path, output_path):
        try:
            stream = ffmpeg.input(input_path)
            stream = ffmpeg.output(
                stream,
                output_path,
                vcodec='libx264',
                acodec='aac',
                preset='fast',
                format='mp4'
            )
            ffmpeg.run(stream)
            return output_path
        except ffmpeg.Error as e:
            raise Exception(f"FFmpeg error during conversion: {e.stderr.decode()}")

    def chunk_video_to_hls(self, input_path, output_prefix):
        output_path = f"{self.output_dir}/{output_prefix}"
        os.makedirs(output_path, exist_ok=True)
        playlist_path = f"{output_path}/playlist.m3u8"
        try:
            stream = ffmpeg.input(input_path)
            stream = ffmpeg.output(
                stream,
                playlist_path,
                format='hls',
                hls_time=10,  # 10-second segments
                hls_list_size=0,
                hls_segment_filename=f"{output_path}/segment_%03d.ts"
            )
            ffmpeg.run(stream)
            segments = [f for f in os.listdir(output_path) if f.endswith('.ts')]
            return playlist_path, segments
        except ffmpeg.Error as e:
            raise Exception(f"FFmpeg error during HLS chunking: {e.stderr.decode()}")

    def extract_thumbnail(self, input_path, output_path):
        try:
            stream = ffmpeg.input(input_path, ss=1)  # Thumbnail at 1 second
            stream = ffmpeg.output(stream, output_path, vframes=1, format='image2', vcodec='mjpeg')
            ffmpeg.run(stream)
            return output_path
        except ffmpeg.Error as e:
            raise Exception(f"FFmpeg error during thumbnail extraction: {e.stderr.decode()}")