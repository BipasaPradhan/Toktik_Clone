package io.muzoo.scalable.vms.redis;

import io.muzoo.scalable.vms.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisMethods {
    @Getter
    private final VideoRepository videoRepository;
    private final RedisPublisher redisPublisher;
    private final RedisTemplate<String, Long> redisTemplate;

    @Transactional
    public void incrementLikeCountRedis(Long videoId) {
        String key = "video:" + videoId + ":likes";
        Long likeCount = redisTemplate.opsForValue().increment(key, 1L);

        Map<String, String> message = Map.of(
                "videoId", videoId.toString(),
                "likeCount", likeCount.toString()
        );
        System.out.println("Publishing like count to Redis: videoId=" + videoId + ", likeCount=" + likeCount);
        redisPublisher.publish("like:count", message);
        try {
            videoRepository.incrementLikeCount(videoId);
            System.out.println("Updated database like count for videoId=" + videoId);
        } catch (Exception e) {
            System.out.println("Failed to update database like count for videoId=" + videoId + ": " + e.getMessage());
        }
    }

    @Transactional
    public void decrementLikeCountRedis(Long videoId) {
        String key = "video:" + videoId + ":likes";
        Long likeCount = redisTemplate.opsForValue().decrement(key);
        if (likeCount < 0) {
            redisTemplate.opsForValue().set(key, 0L);
            likeCount = 0L;
        }

        Map<String, String> message = Map.of(
                "videoId", videoId.toString(),
                "likeCount", likeCount.toString()
        );
        System.out.println("Publishing like count to Redis: videoId=" + videoId + ", likeCount=" + likeCount);
        redisPublisher.publish("like:count", message);
        try {
            videoRepository.decrementLikeCountSafely(videoId);
            System.out.println("Updated database like count for videoId=" + videoId);
        } catch (Exception e) {
            System.out.println("Failed to update database like count for videoId=" + videoId + ": " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000) // Sync every 60 seconds
    @Transactional
    public void syncLikeCountsToDatabase() {
        System.out.println("Syncing like counts to database");
        redisTemplate.keys("video:*:likes").forEach(key -> {
            Long videoId = Long.parseLong(key.split(":")[1]);
            Long likeCount = redisTemplate.opsForValue().get(key);
            if (likeCount != null) {
                try {
                    videoRepository.updateLikeCount(videoId, likeCount); // Use new method
                    System.out.println("Synced like count for videoId=" + videoId + ": " + likeCount);
                } catch (Exception e) {
                    System.out.println("Failed to sync like count for videoId=" + videoId + ": " + e.getMessage());
                }
            }
        });
    }
}
