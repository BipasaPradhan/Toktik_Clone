package io.muzoo.ssc.project.backend;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SimpleResponseDTO {

    private boolean success;
    private String message;
}
