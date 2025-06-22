package io.muzoo.scalable.web_socket.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewCountMessageListener implements MessageListener {
    //    private static final Logger logger = LoggerFactory.getLogger(ViewCountMessageListener.class);
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        System.out.println("Received Redis message on channel: " + new String(pattern));
        try {
            Map<String, String> data = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            Long videoId = Long.parseLong(data.get("video_id"));
            Long viewCount = Long.parseLong(data.get("view_count"));
            System.out.println("Received view:count message: video_id: " + videoId + "view_count: " + viewCount);
            messagingTemplate.convertAndSend("/topic/views/" + videoId, viewCount);
            System.out.println("Sent view count to WebSocket: /topic/views/" + videoId + ", view_count=" + viewCount);
        } catch (Exception e) {
            System.out.println("Error processing view:count message: " + e.getMessage());
        }
    }
}
