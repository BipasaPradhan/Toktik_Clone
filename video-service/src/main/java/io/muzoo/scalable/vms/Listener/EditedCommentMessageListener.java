package io.muzoo.scalable.vms.Listener;

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
public class EditedCommentMessageListener implements MessageListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        System.out.println("Received Redis message on channel: " + new String(pattern));
        try {
            Map<String, Object> data = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            Long videoId = Long.parseLong(data.get("video_id").toString());
            messagingTemplate.convertAndSend("/topic/comment:edited/" + videoId, data);
            System.out.println("Sent comment to WebSocket: /topic/comment:edited/" + videoId);
        } catch (Exception e) {
            System.out.println("Error processing comment:new message: " + e.getMessage());
        }
    }
}
