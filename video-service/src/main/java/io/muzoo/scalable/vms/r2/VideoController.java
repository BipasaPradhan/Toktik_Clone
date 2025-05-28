package io.muzoo.scalable.vms.r2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @GetMapping("/presign-upload")
    public ResponseEntity<String> getPresignedUploadUrl(
            @RequestParam String videoFileName,
            Authentication authentication) {
        System.out.println("Received request for videoFileName: " + videoFileName);
        System.out.println("Authentication: " + authentication);
        String userId = authentication.getName();
        System.out.println("Extracted userId: " + userId);
        if (userId == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        try {
            String presignedUrl = videoService.generatePresignedUploadUrl(videoFileName, userId);
            System.out.println("Generated Presigned URL: " + presignedUrl);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating presigned URL: " + e.getMessage());
        }
    }

    @GetMapping("/presign-download")
    public ResponseEntity<String> getPresignedDownloadUrl(
            @RequestParam String videoFileName,
            Authentication authentication) {
        String userId = authentication.getName();

        try {
            String presignedUrl = videoService.generatePresignedDownloadUrl(videoFileName, userId);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating presigned URL: " + e.getMessage());
        }
    }
}
