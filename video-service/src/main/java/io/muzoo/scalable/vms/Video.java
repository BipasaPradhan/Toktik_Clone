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
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String visibility;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Column(nullable = false)
    private String objectKey;

    @Column
    private String videoUrl;

    @Column
    private String thumbnailUrl;

    @Column
    private String hlsPlaylistUrl;

    @Column
    private Double duration;

    public Video(String userId, String title, String description, String objectKey, VideoStatus status, String visibility) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.objectKey = objectKey;
        this.status = status;
        this.visibility = visibility;
        this.uploadTime = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}