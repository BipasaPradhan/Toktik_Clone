package io.muzoo.scalable.vms.CommentUtils;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Data
@Table(name = "video_comment")
public class VideoComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Video this comment belongs to
    @Column(name = "video_id", nullable = false)
    private Long videoId;

    // User who made the comment
    @Column(name = "user_id", nullable = false)
    private String userId;

    // The actual comment
    @Column(name = "content", nullable = false, columnDefinition = "VARCHAR(500)")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // optional: last edited time if you want to support editing
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    public VideoComment() {}

    public VideoComment(Long videoId, String userId, String content) {
        this.videoId = videoId;
        this.userId = userId;
        this.content = content;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}