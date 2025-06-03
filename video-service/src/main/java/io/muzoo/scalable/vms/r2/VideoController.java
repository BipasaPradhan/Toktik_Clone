package io.muzoo.scalable.vms.r2;

import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @GetMapping("/presign-upload")
    public ResponseEntity<VideoService.PresignedUploadResponse> getPresignedUploadUrl(
            @RequestParam String videoFileName,
            @RequestParam String userId) {
        System.out.println("Received request for videoFileName: " + videoFileName + ", userId: " + userId);
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(400).body(null);
        }
        try {
            VideoService.PresignedUploadResponse response = videoService.generatePresignedUploadUrl(videoFileName, userId);
            System.out.println("Generated Presigned URL: " + response.getPresignedUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/presign-download")
    public ResponseEntity<String> getPresignedDownloadUrl(
            @RequestParam String objectKey,
            @RequestParam String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(400).body("Missing userId");
        }
        try {
            String presignedUrl = videoService.generatePresignedDownloadUrl(objectKey);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating presigned URL: " + e.getMessage());
        }
    }

    @PostMapping("/metadata")
    public ResponseEntity<Map<String, String>> saveVideoMetadata(
            @RequestParam String objectKey,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String visibility,
            @RequestParam String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Missing userId"));
        }
        try {
            Video video = videoService.saveVideoMetadata(userId, objectKey, title, description, visibility);
            return ResponseEntity.ok(Map.of("videoId", video.getId().toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error saving metadata: " + e.getMessage()));
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getVideoFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (page < 1 || size < 1) {
                return ResponseEntity.status(400).body(Map.of("error", "Page and size must be positive integers"));
            }
            List<Video> videos = videoService.getVideoFeed(page, size);
            List<Map<String, Object>> videoList = videos.stream().map(video -> {
                Map<String, Object> videoInfo = new HashMap<>();
                videoInfo.put("id", video.getId());
                videoInfo.put("title", video.getTitle());
                videoInfo.put("thumbnailUrl", video.getThumbnailUrl());
                videoInfo.put("userId", video.getUserId());
                videoInfo.put("uploadTime", video.getUploadTime().toString());
                return videoInfo;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("videos", videoList));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching video feed: " + e.getMessage()));
        }
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getVideoDetails(
            @RequestParam Long videoId,
            @RequestParam String userId) {
        if (videoId == null || userId == null || userId.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Missing videoId or userId"));
        }
        try {
            VideoService.VideoDetailsResponse response = videoService.getVideoDetails(videoId, userId);
            Map<String, Object> details = new HashMap<>();
            details.put("hlsUrl", response.getHlsUrl());
            details.put("thumbnailUrl", response.getThumbnailUrl());
            details.put("convertedUrl", response.getConvertedUrl());
            details.put("title", response.getTitle());
            details.put("description", response.getDescription());
            details.put("userId", response.getUserId());
            details.put("duration", response.getDuration());
            return ResponseEntity.ok(details);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching video details: " + e.getMessage()));
        }
    }
}