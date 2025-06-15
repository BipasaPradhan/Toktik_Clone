package io.muzoo.scalable.vms;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Data
@NoArgsConstructor
@Table(name = "vms_video_data")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, name = "title")
    private String title;

    @Column(nullable = false, name = "description")
    private String description;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false, name = "visibility")
    private String visibility;

    @Column(nullable = false, updatable = false, name = "upload_time")
    private LocalDateTime uploadTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private VideoStatus status;

    @Column(nullable = false, name = "object_key")
    private String objectKey;

    @Column(name = "hls_playlist_url")
    private String hlsPlaylistUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "chunked_url")
    private String chunkedUrl;

    @Column(name = "duration")
    private Double duration;

    @Column(name = "view_count", nullable = false, columnDefinition = "LONG DEFAULT 0")
    private Long viewCount = 0L;

    public Video(String userId, String title, String description, String objectKey, VideoStatus status, String visibility) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.objectKey = objectKey;
        this.status = status;
        this.visibility = visibility;
        this.uploadTime = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        this.viewCount = 0L;
    }
}