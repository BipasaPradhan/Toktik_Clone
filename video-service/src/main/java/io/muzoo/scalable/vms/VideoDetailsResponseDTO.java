package io.muzoo.scalable.vms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoDetailsResponseDTO {

    private final String hlsUrl;
    private final String thumbnailUrl;
    private final String convertedUrl;
    private final String title;
    private final String description;
    private final String userId;
    private final Double duration;
}