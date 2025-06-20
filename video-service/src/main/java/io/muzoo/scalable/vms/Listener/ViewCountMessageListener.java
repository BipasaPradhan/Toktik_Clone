package io.muzoo.scalable.vms.Listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewCountMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ViewCountMessageListener.class);
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void onMessage(Message message, byte[] pattern)  {
        try{
            // Deserialize the message body (JSON) into a Map
            Map<String, String> data = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            Long videoId = Long.parseLong(data.get("video_id"));
            Long viewCount = Long.parseLong(data.get("view_count"));
            logger.info("Received view:count message: video_id={}, view_count={}", videoId, viewCount);

            // Broadcast to WebSocket clients subscribed to this video
            messagingTemplate.convertAndSend("/topic/views/" + videoId, viewCount);
        }
        catch (Exception e) {
            logger.error("Error processing view:count message: {}", e.getMessage(), e);
        }
    }
}
