package io.muzoo.scalable.vms.CommentUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private Long videoId;
    private String userId;
    private String content;
    private String createdAt;
}
