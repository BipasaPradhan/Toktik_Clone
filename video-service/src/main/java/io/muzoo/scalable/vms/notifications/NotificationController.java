package io.muzoo.scalable.vms.notifications;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class NotificationController {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID required"));
        }

        String notificationKey = "notifications:user:" + userId;
        List<Object> rawList = redisTemplate.opsForList().range(notificationKey, 0, -1);
        if (rawList == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, String>> result = rawList.stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), new TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        return Map.of("message", "Error parsing notification", "timestamp", "", "read", "false");
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }



}
