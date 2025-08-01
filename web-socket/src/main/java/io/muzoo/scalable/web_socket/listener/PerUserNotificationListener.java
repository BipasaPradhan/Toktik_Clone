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
public class PerUserNotificationListener implements MessageListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        System.out.println("Received Redis message on channel: " + new String(pattern));
        try {
            Map<String, Object> data = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            String userId = (String) data.get("userId");

            data.put("read", Boolean.parseBoolean(String.valueOf(data.get("read"))));

            if (userId != null) {
                messagingTemplate.convertAndSend("/user/" + userId + "/notifications", data);
                System.out.println("Notified user " + userId + " via WebSocket: " + data);
            } else {
                System.out.println("Invalid notification data: missing userId");
            }
        } catch (Exception e) {
            System.out.println("Error in PerUserNotificationListener: " + e.getMessage());
        }
    }

}
