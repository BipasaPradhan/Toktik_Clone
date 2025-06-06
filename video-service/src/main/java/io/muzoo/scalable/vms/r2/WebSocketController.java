package io.muzoo.scalable.vms.r2;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
@CrossOrigin(origins = "http://localhost:3000")
public class WebSocketController {

    @MessageMapping("/video/processed")
    @SendTo("/topic/video-updates")
    public VideoUpdateMessage sendVideoUpdate(VideoUpdateMessage message) {
        System.out.println("Sending update for videoId: " + message.getVideoId());
        return message; // sends message to subscribers
    }

    public static class VideoUpdateMessage {
        private String videoId;
        private String hlsUrl;
        private String thumbnailUrl;
        private String convertedUrl;
        private Double duration;

        public VideoUpdateMessage() {}

        public VideoUpdateMessage(String videoId, String hlsUrl, String thumbnailUrl, String convertedUrl, Double duration) {
            this.videoId = videoId;
            this.hlsUrl = hlsUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.convertedUrl = convertedUrl;
            this.duration = duration;
        }

        // Getters and setters
        public String getVideoId() { return videoId; }
        public void setVideoId(String videoId) { this.videoId = videoId; }
        public String getHlsUrl() { return hlsUrl; }
        public void setHlsUrl(String hlsUrl) { this.hlsUrl = hlsUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public String getConvertedUrl() { return convertedUrl; }
        public void setConvertedUrl(String convertedUrl) { this.convertedUrl = convertedUrl; }
        public Double getDuration() { return duration; }
        public void setDuration(Double duration) { this.duration = duration; }
    }
}