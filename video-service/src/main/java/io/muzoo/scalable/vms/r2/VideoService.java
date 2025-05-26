package io.muzoo.scalable.vms.r2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
public class VideoService {
    private final S3Presigner s3Presigner;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    public VideoService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

}
