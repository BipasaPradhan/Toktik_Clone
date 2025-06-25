package io.muzoo.scalable.vms.notifications;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.CommentUtils.HtmlSanitizer;
import io.muzoo.scalable.vms.CommentUtils.VideoCommentRepository;
import io.muzoo.scalable.vms.Video;
import io.muzoo.scalable.vms.VideoRepository;
import io.muzoo.scalable.vms.redis.RedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final VideoRepository videoRepository;
    private final RedisPublisher redisPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public void notifyVipUsersComment(Long videoId, String actorUserId) {
        String vipKey = "video:" + videoId + ":vips";
        Set<String> vipUserIds = Optional.ofNullable(stringRedisTemplate.opsForSet().members(vipKey))
                .orElse(Collections.emptySet())
                .stream()
                .map(Object::toString)
                .filter(id -> !id.equals(actorUserId))
                .collect(Collectors.toSet());

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with ID: " + videoId));
        String videoTitle = video.getTitle();

        String message = String.format("User %s commented on video '%s'", actorUserId, videoTitle);

        for (String userId : vipUserIds) {
            String notificationKey = "notifications:user:" + userId;

            Map<String, Object> notification = Map.of(
                    "userId", userId,
                    "videoId", videoId,
                    "message", message,
                    "timestamp", Instant.now().toString(),
                    "read", false
            );

            try {
                String notificationJson = objectMapper.writeValueAsString(notification);
                stringRedisTemplate.opsForList().rightPush(notificationKey, notificationJson);
                stringRedisTemplate.opsForList().trim(notificationKey, -50, -1);

                String redisChannel = "notification:user:" + userId;
                redisPublisher.publishString(redisChannel, notificationJson);
                System.out.println("Published notification to channel " + redisChannel + ": " + message);
            } catch (JsonProcessingException e) {
                System.out.println("Error serializing notification JSON: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error storing or publishing notification: " + e.getMessage());
            }

        }
    }
}
