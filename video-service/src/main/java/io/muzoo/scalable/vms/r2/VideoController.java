package io.muzoo.scalable.vms.r2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
