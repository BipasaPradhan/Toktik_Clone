package io.muzoo.scalable.vms.comments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.CommentUtils.*;
import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoRepository;
import io.muzoo.scalable.vms.notifications.NotificationService;
import io.muzoo.scalable.vms.redis.RedisPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoCommentService {
    private final VideoCommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final RedisPublisher redisPublisher;
    private final HtmlSanitizer htmlSanitizer;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationService notificationService;

    @Transactional
    public CommentResponseDTO addComment(Long videoId, String userId, AddCommentRequestDTO request) {
        videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with ID: " + videoId));
        String sanitizedContent = htmlSanitizer.sanitize(request.getContent());
        VideoComment comment = new VideoComment(videoId, userId, sanitizedContent);
        VideoComment saved = commentRepository.save(comment);

        Map<String, String> message = Map.of(
                "id", saved.getId().toString(),
                "video_id", videoId.toString(),
                "user_id", userId,
                "content", sanitizedContent,
                "created_at", saved.getCreatedAt().toString()
        );
        redisPublisher.publish("comment:new", message);

        // Add user to VIP set
        String vipKey = "video:" + videoId + ":vips";
        Long added = stringRedisTemplate.opsForSet().add(vipKey, userId);

        if (added != null && added == 1L) {
            System.out.println("User " + userId + " added to VIP set for video " + videoId);
        } else {
            System.out.println("User " + userId + " was already a VIP for video " + videoId);
        }

        // Trigger notification for VIPs
        notificationService.notifyVipUsersComment(videoId, userId);

        return new CommentResponseDTO(
                saved.getId(),
                saved.getVideoId(),
                saved.getUserId(),
                sanitizedContent,
                saved.getCreatedAt().toString()
        );
    }

    public List<CommentResponseDTO> getCommentsForVideo(Long videoId) {
        System.out.println("Fetching comments for videoId: " + videoId);
        return commentRepository.findByVideoIdOrderByCreatedAtAsc(videoId)
                .stream()
                .map(comment -> new CommentResponseDTO(
                        comment.getId(),
                        comment.getVideoId(),
                        comment.getUserId(),
                        comment.getContent(),
                        comment.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

}
