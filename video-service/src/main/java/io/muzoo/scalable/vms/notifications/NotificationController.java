package io.muzoo.scalable.vms.notifications;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class NotificationController {
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // Get notifications for the user
    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, Object>>> getNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.emptyList());
        }

        String notificationKey = "notifications:user:" + userId;
        List<String> rawList = stringRedisTemplate.opsForList().range(notificationKey, 0, -1);
        if (rawList == null) rawList = Collections.emptyList();

        List<Map<String, Object>> result = rawList.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                    } catch (Exception e) {
                        System.out.println("Error parsing notification: " + e.getMessage());
                        Map<String, Object> fallback = new HashMap<>();
                        fallback.put("message", "Error parsing notification");
                        fallback.put("timestamp", "");
                        fallback.put("read", false);
                        return fallback;
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // Mark a specific notification as read (by index)
    @PostMapping("/notifications/{index}/read")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable int index) {

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID required"));
        }

        String notificationKey = "notifications:user:" + userId;
        String rawNotification = stringRedisTemplate.opsForList().index(notificationKey, index);
        if (rawNotification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notification not found at index " + index));
        }

        try {
            Map<String, Object> notification = objectMapper.readValue(rawNotification, new TypeReference<>() {});
            notification.put("read", true);
            String updatedJson = objectMapper.writeValueAsString(notification);
            stringRedisTemplate.opsForList().set(notificationKey, index, updatedJson);
            return ResponseEntity.ok(Map.of("message", "Marked as read", "index", index));
        } catch (Exception e) {
            System.out.println("Error updating notification at index " + index + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating notification"));
        }
    }

}

