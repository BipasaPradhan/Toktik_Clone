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
import java.util.List;
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

    // DTO for video details response
    @Getter
    public static class VideoDetailsResponse {
        private final String hlsUrl;
        private final String thumbnailUrl;
        private final String convertedUrl;
        private final String title;
        private final String description;
        private final String userId;
        private final Double duration;

        public VideoDetailsResponse(String hlsUrl, String thumbnailUrl, String convertedUrl, String title, String description, String userId, Double duration) {
            this.hlsUrl = hlsUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.convertedUrl = convertedUrl;
            this.title = title;
            this.description = description;
            this.userId = userId;
            this.duration = duration;
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

        // Publish to Redis Pub/Sub
        String videoId = savedVideo.getId().toString(); // Use database ID
        Map<String, String> message = Map.of(
                "video_id", videoId,
                "s3_key", objectKey,
                "user_id", userId
        );
        System.out.println("Publishing to video:process channel: video_id=" + videoId + ", s3_key=" + objectKey);
        redisPublisher.publish("video:process", message);

        return savedVideo;
    }

    // Update metadata after workers
    @Transactional
    public Video updateVideoMetadata(Long videoId, String hlsPlaylistUrl, String thumbnailUrl, String convertedUrl, Double duration) {
        System.out.println("Updating metadata for videoId: " + videoId + ", hlsPlaylistUrl: " + hlsPlaylistUrl +
                ", thumbnailUrl: " + thumbnailUrl + ", duration: " + duration);
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    System.out.println("Video not found with ID: " + videoId);
                    return new IllegalArgumentException("Video not found with ID: " + videoId);
                });
        video.setHlsPlaylistUrl(hlsPlaylistUrl);
        video.setThumbnailUrl(thumbnailUrl);
        video.setChunkedUrl(convertedUrl);
        video.setDuration(duration); // if duration is there
        video.setStatus(VideoStatus.UPLOADED);
        Video updatedVideo = videoRepository.save(video);
        System.out.println("Updated video metadata for ID: " + videoId + ", status: " + VideoStatus.UPLOADED);
        return updatedVideo;
    }

    public List<Video> getVideoFeed(int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Page and size must be positive integers");
        }
        int offset = (page - 1) * size;
        return videoRepository.findByVisibilityAndStatus("Public", VideoStatus.UPLOADED, size, offset);
    }

    public VideoDetailsResponse getVideoDetails(Long videoId, String userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with ID: " + videoId));

        if (!"Public".equals(video.getVisibility()) && !video.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: Video is private or not owned by the user");
        }

        // Generate presigned URLs
        String hlsUrl = generatePresignedDownloadUrl(video.getHlsPlaylistUrl());
        String thumbnailUrl = generatePresignedDownloadUrl(video.getThumbnailUrl());
        String convertedUrl = video.getChunkedUrl() != null ? generatePresignedDownloadUrl(video.getChunkedUrl()) : null;

        return new VideoDetailsResponse(hlsUrl, thumbnailUrl, convertedUrl, video.getTitle(), video.getDescription(), video.getUserId(), video.getDuration());
    }
}