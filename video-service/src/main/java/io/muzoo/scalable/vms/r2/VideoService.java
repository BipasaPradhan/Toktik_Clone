package io.muzoo.scalable.vms.r2;

import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoRepository;
import io.muzoo.scalable.vms.VideoStatus;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class VideoService {
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final VideoRepository videoRepository;

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

    // Presigned URL for uploading (PUT)
    public PresignedUploadResponse generatePresignedUploadUrl(String videoFileName, String userId) {
        String baseKey = userId + "/" + videoFileName;
        String uniqueKey = baseKey + "_" + System.currentTimeMillis();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUploadResponse(presignedRequest.url().toString(), uniqueKey);
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

        VideoStatus status = VideoStatus.UPLOADED;
        Video video = new Video(userId, title, description, objectKey, status, visibility);
        return videoRepository.save(video);
    }
}
