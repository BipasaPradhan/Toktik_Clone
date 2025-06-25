package io.muzoo.scalable.vms.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }

    public void publishString(String channel, String message) {
        stringRedisTemplate.convertAndSend(channel, message);
    }
}
