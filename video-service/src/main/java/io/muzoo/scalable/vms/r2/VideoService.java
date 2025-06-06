package io.muzoo.scalable.vms.r2;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoRepository;
import io.muzoo.scalable.vms.VideoStatus;
import io.muzoo.scalable.vms.VideoDetailsResponseDTO;
import io.muzoo.scalable.vms.redis.RedisPublisher;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    @Getter
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
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uniqueFileName = videoFileName.replace(".mp4", "_" + timestamp + ".mp4");
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

    public String generatePresignedDownloadUrl(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            System.out.println("Skipping presigned URL generation: objectKey is null or empty");
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
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

        VideoStatus status = VideoStatus.PROCESSING;
        Video video = new Video(userId, title, description, objectKey, status, visibility);
        Video savedVideo = videoRepository.save(video);
        String videoId = savedVideo.getId().toString();
        Map<String, String> message = Map.of("video_id", videoId, "s3_key", objectKey, "user_id", userId);
        System.out.println("Publishing to video:process channel: video_id=" + videoId + ", s3_key=" + objectKey);
        redisPublisher.publish("video:process", message);
        return savedVideo;
    }

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
        video.setDuration(duration);
        video.setStatus(VideoStatus.UPLOADED);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Video updatedVideo = videoRepository.save(video);
        System.out.println("Updated video metadata for ID: " + videoId + ", status: " + VideoStatus.UPLOADED);
        return updatedVideo;
    }

    public List<Video> getVideoFeed(int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Page and size must be positive integers");
        }
        int offset = (page - 1) * size;
        return videoRepository.findByVisibilityAndStatus("Public", size, offset);
    }

    @Transactional
    public VideoDetailsResponseDTO getVideoDetails(Long videoId, String userId) {
        System.out.println("Attempting to fetch video with ID: " + videoId + " for userId: " + userId);
        Video video = videoRepository.findByIdNative(videoId)
                .orElseThrow(() -> {
                    System.out.println("Database query for videoId=" + videoId + " returned no result");
                    return new IllegalArgumentException("Video not found with ID: " + videoId);
                });
        System.out.println("Found video: id=" + video.getId() + ", title=" + video.getTitle() +
                ", visibility=" + video.getVisibility() + ", hlsPlaylistUrl=" + video.getHlsPlaylistUrl() +
                ", thumbnailUrl=" + video.getThumbnailUrl());

        if (!"Public".equals(video.getVisibility()) && !video.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: Video is private or not owned by the user");
        }

        String hlsPlaylistContent = getRewrittenHlsPlaylist(video.getHlsPlaylistUrl());
        String thumbnailUrl = generatePresignedDownloadUrl(video.getThumbnailUrl());
        String convertedUrl = video.getChunkedUrl() != null ? generatePresignedDownloadUrl(video.getChunkedUrl()) : null;

        System.out.println("Generated hlsPlaylistContent (first 100 chars): " +
                hlsPlaylistContent.substring(0, Math.min(100, hlsPlaylistContent.length())));
        System.out.println("Generated thumbnailUrl: " + thumbnailUrl);

        VideoDetailsResponseDTO responseDTO = VideoDetailsResponseDTO.builder()
                .hlsUrl(hlsPlaylistContent)
                .hlsKey(video.getHlsPlaylistUrl())
                .thumbnailUrl(thumbnailUrl)
                .convertedUrl(convertedUrl)
                .title(video.getTitle())
                .description(video.getDescription())
                .userId(video.getUserId())
                .duration(video.getDuration())
                .uploadTime(video.getUploadTime() != null ? video.getUploadTime().toString() : null)
                .build();

        System.out.println("Returning DTO with hlsKey: " + responseDTO.getHlsKey());
        return responseDTO;
    }

    private String getRewrittenHlsPlaylist(String hlsPlaylistKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(hlsPlaylistKey)
                    .build();
            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(response))) {
                String playlistContent = reader.lines().collect(Collectors.joining("\n"));
                String basePath = hlsPlaylistKey.substring(0, hlsPlaylistKey.lastIndexOf('/') + 1);
                StringBuilder rewrittenPlaylist = new StringBuilder();
                for (String line : playlistContent.split("\n")) {
                    if (line.endsWith(".ts")) {
                        String segmentKey = basePath + line;
                        String presignedSegmentUrl = generatePresignedDownloadUrl(segmentKey);
                        rewrittenPlaylist.append(presignedSegmentUrl).append("\n");
                        System.out.println("Rewrote segment URL: " + line + " -> " + presignedSegmentUrl);
                    } else {
                        rewrittenPlaylist.append(line).append("\n");
                    }
                }
                return rewrittenPlaylist.toString();
            }
        } catch (NoSuchKeyException e) {
            System.out.println("HLS playlist not found: " + hlsPlaylistKey);
            throw new IllegalArgumentException("HLS playlist not found: " + hlsPlaylistKey);
        } catch (S3Exception e) {
            System.out.println("Error fetching HLS playlist: " + e.getMessage());
            throw new RuntimeException("Error fetching HLS playlist: " + e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("Unexpected error reading HLS playlist: " + e.getMessage());
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }

    public List<Video> getMyVideos(int page, int size, String userId) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Page and size must be positive integers");
        }
        int offset = (page - 1) * size;
        return videoRepository.findByUserIdAndStatus(userId, size, offset);
    }
}