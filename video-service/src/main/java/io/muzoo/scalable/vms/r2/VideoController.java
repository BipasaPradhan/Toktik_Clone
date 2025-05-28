package io.muzoo.scalable.vms.r2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8000", "http://localhost/"})
public class VideoController {
    private final VideoService videoService;

    @GetMapping("/presign-upload")
    public ResponseEntity<String> getPresignedUploadUrl(
            @RequestParam String videoFileName,
            @RequestParam String userId) {
//        // Validate user authentication
//        if (!isAuthenticatedUser(userId)) {
//            return ResponseEntity.status(401).body("Unauthorized: Invalid user");
//        }
//
        try {
            String presignedUrl = videoService.generatePresignedUploadUrl(videoFileName, userId);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating presigned URL: " + e.getMessage());
        }
    }

    @GetMapping("/presign-download")
    public ResponseEntity<String> getPresignedDownloadUrl(
            @RequestParam String videoFileName,
            @RequestParam String userId) {
//        // Validate user authentication
//        if (!isAuthenticatedUser(userId)) {
//            return ResponseEntity.status(401).body("Unauthorized: Invalid user");
//        }

        try {
            String presignedUrl = videoService.generatePresignedDownloadUrl(videoFileName, userId);
            return ResponseEntity.ok(presignedUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error generating presigned URL: " + e.getMessage());
        }
    }


}
