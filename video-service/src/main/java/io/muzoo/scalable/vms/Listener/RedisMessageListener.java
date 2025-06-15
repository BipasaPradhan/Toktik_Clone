package io.muzoo.scalable.vms.Listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.r2.VideoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisMessageListener.class);
    private final VideoService videoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Deserialize the message body (JSON) into a Map
            Map<String, String> data = objectMapper.readValue(message.getBody(), new TypeReference<>() {});
            Long videoId = Long.parseLong(data.get("video_id"));
            String hlsPlaylistUrl = data.get("hls_playlist_url");
            String thumbnailUrl = data.get("thumbnail_url");
            String convertedUrl = data.get("converted_url");
            Double duration = data.get("duration") != null ? Double.parseDouble(data.get("duration")) : null;

            logger.info("Received video:processed message: video_id={}, hlsPlaylistUrl={}, thumbnailUrl={}, convertedUrl={} duration={}",
                    videoId, hlsPlaylistUrl, thumbnailUrl, convertedUrl, duration);

            // Update video metadata
            videoService.updateVideoMetadata(videoId, hlsPlaylistUrl, thumbnailUrl, convertedUrl, duration);
        } catch (Exception e) {
            logger.error("Error processing video:processed message: {}", e.getMessage(), e);
        }
    }
}