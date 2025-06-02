package io.muzoo.scalable.vms.r2;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoRepository;
import io.muzoo.scalable.vms.VideoStatus;
import io.muzoo.scalable.vms.redis.RedisPublisher;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final VideoRepository videoRepository;
    private final RedisPublisher redisPublisher;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    // Class to hold presigned URL and objectKey
    @Getter
    public static class PresignedUploadResponse {
        private final String presignedUrl;
        private final String objectKey;

        public PresignedUploadResponse(String presignedUrl, String objectKey) {
            this.presignedUrl = presignedUrl;
            this.objectKey = objectKey;
        }
    }

    public PresignedUploadResponse generatePresignedUploadUrl(String videoFileName, String userId) {
        // Ensure videoFileName is used as the base name, appending timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueFileName = videoFileName.replace(".mp4", "_" + timestamp + ".mp4"); // e.g., video_1234567890.mp4
        String baseKey = userId + "/output/" + uniqueFileName;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(baseKey)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUploadResponse(presignedRequest.url().toString(), baseKey);
    }

    // Presigned URL for downloading (GET)
    public String generatePresignedDownloadUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Transactional
    public Video saveVideoMetadata(String userId, String objectKey, String title, String description, String visibility) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("Video file not found in storage");
        } catch (S3Exception e) {
            throw new RuntimeException("Error checking object in R2: " + e.getMessage(), e);
        }

        VideoStatus status = VideoStatus.PROCESSING; //processing once uploaded whole OG file to r2
        Video video = new Video(userId, title, description, objectKey, status, visibility);
        Video savedVideo = videoRepository.save(video);

//        // Enqueue Celery task for video processing
//        try {
//            Jedis jedis = new Jedis("redis", 6379);
//            Map<String, String> taskData = new HashMap<>();
//            taskData.put("video_id", "video_" + savedVideo.getId());
//            taskData.put("s3_key", objectKey);
//            String taskJson = new ObjectMapper().writeValueAsString(taskData);
//
//            // Push task to Celery queue
//            jedis.lpush("celery", "{\"id\": \"video_" + savedVideo.getId() + "\", \"task\": \"tasks.process_video_task\", \"args\": [" + taskJson + "], \"kwargs\": {}, \"retries\": 0}");
//            jedis.close();
//        } catch (Exception e) {
//            throw new RuntimeException("Error enqueuing video processing task: " + e.getMessage(), e);
//        }
//
//        return savedVideo;

        // Publish to Redis Pub/Sub
        String videoId = savedVideo.getId().toString(); // Use database ID
        Map<String, String> message = Map.of(
                "video_id", videoId,
                "s3_key", objectKey
        );
        System.out.println("Publishing to video:process channel: video_id=" + videoId + ", s3_key=" + objectKey);
        redisPublisher.publish("video:process", message);

        return savedVideo;
    }

    // Update metadata after workers
    @Transactional
    public Video updateVideoMetadata(Long videoId, String hlsPlaylistUrl, String thumbnailUrl, Double duration) {
        System.out.println("Updating metadata for videoId: " + videoId + ", hlsPlaylistUrl: " + hlsPlaylistUrl +
                ", thumbnailUrl: " + thumbnailUrl + ", duration: " + duration);
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    System.out.println("Video not found with ID: " + videoId);
                    return new IllegalArgumentException("Video not found with ID: " + videoId);
                });
        video.setHlsPlaylistUrl(hlsPlaylistUrl);
        video.setThumbnailUrl(thumbnailUrl);
        // video.setDuration(duration); // if duration is there
        video.setStatus(VideoStatus.UPLOADED);
        Video updatedVideo = videoRepository.save(video);
        System.out.println("Updated video metadata for ID: " + videoId + ", status: " + VideoStatus.UPLOADED);
        return updatedVideo;
    }
}
