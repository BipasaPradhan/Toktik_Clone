package io.muzoo.scalable.vms.Listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeCountMessageListener implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("Received Redis message on channel: {}", new String(pattern));
        try {
            Map<String, String> data = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            Long videoId = Long.parseLong(data.get("videoId"));
            Long likeCount = Long.parseLong(data.get("likeCount"));
            log.info("Received like:count message: videoId={}, likeCount={}", videoId, likeCount);
            messagingTemplate.convertAndSend("/topic/likes/" + videoId, likeCount);
            log.info("Sent like count to WebSocket: /topic/likes/{}, likeCount={}", videoId, likeCount);
        } catch (Exception e) {
            log.error("Error processing like:count message: {}", e.getMessage(), e);
        }
    }
}