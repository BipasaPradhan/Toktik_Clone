package io.muzoo.scalable.vms.CommentUtils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddCommentRequestDTO {
    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String content;
}
