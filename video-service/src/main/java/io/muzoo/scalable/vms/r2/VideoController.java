package io.muzoo.scalable.vms.r2;

import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoDetailsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
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
    public ResponseEntity<String> saveVideoMetadata(
            @RequestParam String objectKey,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String visibility,
            @RequestParam String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(400).body("Missing userId");
        }
        try {
            videoService.saveVideoMetadata(userId, objectKey, title, description, visibility);
            return ResponseEntity.ok("Metadata saved successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving metadata: " + e.getMessage());
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getVideoFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoService.getVideoFeed(page, size).stream()
                .map(v -> {
                    Map<String, Object> videoInfo = new HashMap<>();
                    videoInfo.put("id", v.getId());
                    videoInfo.put("title", v.getTitle());
                    videoInfo.put("thumbnailUrl", videoService.generatePresignedDownloadUrl(v.getThumbnailUrl()));
                    videoInfo.put("userId", v.getUserId());
                    videoInfo.put("uploadTime", v.getUploadTime().toString());
                    return videoInfo;
                })
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoService.getMyVideos(page, size, userId).stream()
                .map(v -> {
                    Map<String, Object> videoInfo = new HashMap<>();
                    videoInfo.put("id", v.getId());
                    videoInfo.put("title", v.getTitle());
                    videoInfo.put("description", v.getDescription());
                    videoInfo.put("visibility", v.getVisibility());
                    videoInfo.put("thumbnailUrl", videoService.generatePresignedDownloadUrl(v.getThumbnailUrl()));
                    videoInfo.put("uploadTime", v.getUploadTime().toString());
                    videoInfo.put("userId", v.getUserId());
                    return videoInfo;
                })
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details")
    public ResponseEntity<VideoDetailsResponseDTO> getVideoDetails(
            @RequestParam("videoId") Long videoId,
            @RequestParam("userId") String userId) {
        VideoDetailsResponseDTO videoDetails = videoService.getVideoDetails(videoId, userId);
        return ResponseEntity.ok(videoDetails);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateVideoMetadata(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates,
            @RequestHeader("X-User-Id") String userId) {
        Video video = videoService.getVideoRepository().findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found with ID: " + id));
        if (!video.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to edit this video");
        }
        if (updates.containsKey("title")) video.setTitle(updates.get("title"));
        if (updates.containsKey("description")) video.setDescription(updates.get("description"));
        if (updates.containsKey("visibility")) video.setVisibility(updates.get("visibility"));
        videoService.getVideoRepository().save(video);
        return ResponseEntity.ok().build();
    }
}