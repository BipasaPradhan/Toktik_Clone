package io.muzoo.scalable.vms.r2;

import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoDetailsResponseDTO;
import io.muzoo.scalable.vms.VideoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                    videoInfo.put("viewCount", v.getViewCount());
                    videoInfo.put("likeCount", videoService.getLikeCount(v.getId()));
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
        List<VideoDetailsResponseDTO> videos = videoService.getMyVideos(page, size, userId).stream()
                .map(video -> {
                    String thumbnailUrl = video.getStatus() == VideoStatus.UPLOADED
                            ? videoService.generatePresignedDownloadUrl(video.getThumbnailUrl())
                            : null;
                    String convertedUrl = video.getStatus() == VideoStatus.UPLOADED && video.getChunkedUrl() != null
                            ? videoService.generatePresignedDownloadUrl(video.getChunkedUrl())
                            : null;
                    return VideoDetailsResponseDTO.builder()
                            .id(video.getId())
                            .hlsUrl(video.getStatus() == VideoStatus.UPLOADED ? video.getHlsPlaylistUrl() : null)
                            .hlsKey(video.getHlsPlaylistUrl())
                            .thumbnailUrl(thumbnailUrl)
                            .convertedUrl(convertedUrl)
                            .title(video.getTitle())
                            .description(video.getDescription())
                            .userId(video.getUserId())
                            .duration(video.getDuration())
                            .uploadTime(video.getUploadTime().toString())
                            .status(video.getStatus())
                            .visibility(video.getVisibility())
                            .viewCount(video.getViewCount())
                            .likeCount(videoService.getLikeCount(video.getId()))
                            .build();
                })
                .collect(Collectors.toList());
        response.put("videos", videos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details")
    public ResponseEntity<VideoDetailsResponseDTO> getVideoDetails(
            @RequestParam("videoId") Long videoId,
            @RequestParam("userId") String userId) {
        System.out.println("Received request for /api/videos/details with videoId=" + videoId + ", userId=" + userId);
        try {
            VideoDetailsResponseDTO videoDetails = videoService.getVideoDetails(videoId, userId);
            System.out.println("Successfully fetched video details for videoId=" + videoId);
            return ResponseEntity.ok(videoDetails);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IllegalArgumentException: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (SecurityException e) {
            System.out.println("Caught SecurityException: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            System.out.println("Caught unexpected exception: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e);
        }
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

    @PostMapping("/{id}/views")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        System.out.println("Received request to increment view count for videoId=" + id);
        videoService.incrementViewCount(id);
        System.out.println("Incremented view count for videoId=" + id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id, @RequestHeader("X-User-Id") String userId) {
        videoService.deleteVideo(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        Map<String, Object> response = videoService.toggleLike(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/is-liked")
    public ResponseEntity<Map<String, Object>> isLiked(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", videoService.isLikedByUser(id, userId));
        response.put("likeCount", videoService.getLikeCount(id));
        return ResponseEntity.ok(response);
    }
}