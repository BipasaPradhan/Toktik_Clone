package io.muzoo.scalable.vms.r2;

import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoRepository;
import io.muzoo.scalable.vms.VideoStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Autowired
    private VideoRepository videoRepository;

    // Presigned URL for uploading (PUT)
    public String generatePresignedUploadUrl(String videoFileName, String userId) {
        String key = userId + "/" + videoFileName;

        // Check for duplicate in database
        Optional<Video> existingVideo = videoRepository.findByObjectKey(key);
        if (existingVideo.isPresent()) {
            // Verify if the file actually exists in R2
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
                // File exists in R2, block the upload
                throw new IllegalArgumentException("You have already uploaded this video.");
            } catch (NoSuchKeyException e) {
                // File doesn't exist in R2, delete the database entry to allow re-upload
                videoRepository.delete(existingVideo.get());
            } catch (S3Exception e) {
                // Handle other S3 exceptions (e.g., access denied)
                throw new RuntimeException("Error checking object in R2: " + e.getMessage(), e);
            }
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    // Presigned URL for downloading (GET)
    public String generatePresignedDownloadUrl(String videoFileName, String userId) {
        String key = userId + "/" + videoFileName;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Transactional
    public Video saveVideoMetadata(String userId, String videoFileName, String title, String description, String visibility) {
        String objectKey = userId + "/" + videoFileName;

        // Verify file exists in R2
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

        VideoStatus status = VideoStatus.UPLOADED;
        Video video = new Video(userId, title, description, objectKey, status, visibility);
        return videoRepository.save(video);
    }



}
