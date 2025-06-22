package io.muzoo.scalable.vms;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Data
@NoArgsConstructor
@Table(
        name = "video_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "user_id"})
)
public class VideoLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    public VideoLike(Long videoId, String userId) {
        this.videoId = videoId;
        this.userId = userId;
    }
}