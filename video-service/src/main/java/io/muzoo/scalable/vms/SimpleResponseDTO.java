package io.muzoo.scalable.vms;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SimpleResponseDTO {

    private boolean success;
    private String message;
}
