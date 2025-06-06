package io.muzoo.scalable.vms;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoDetailsResponseDTO {
    @JsonProperty("hlsUrl")
    private final String hlsUrl;
    @JsonProperty("hlsKey")
    private final String hlsKey; // Added
    @JsonProperty("thumbnailUrl")
    private final String thumbnailUrl;
    @JsonProperty("convertedUrl")
    private final String convertedUrl;
    @JsonProperty("title")
    private final String title;
    @JsonProperty("description")
    private final String description;
    @JsonProperty("userId")
    private final String userId;
    @JsonProperty("duration")
    private final Double duration;
    @JsonProperty("uploadTime")
    private final String uploadTime;
    @JsonProperty("status")
    private final VideoStatus status;
}