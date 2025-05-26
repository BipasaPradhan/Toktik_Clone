package com.example.securingweb;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimpleResponseDTO {
    private boolean success;
    private String message;
}
