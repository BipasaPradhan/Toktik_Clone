package io.muzoo.scalable.vms.comments;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.CommentUtils.AddCommentRequestDTO;
import io.muzoo.scalable.vms.CommentUtils.CommentResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoCommentController {

    private final VideoCommentService commentService;

    @PostMapping("/{videoId}/comments")
    public ResponseEntity<?> postComment(
            @PathVariable Long videoId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody AddCommentRequestDTO request) {
        if (userId == null || userId.trim().isEmpty()) {
            System.out.println("Unauthorized: Missing or empty X-User-Id for videoId: " + videoId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User ID required"));
        }
        System.out.println("Received request to post comment for videoId: " + videoId + " by userId: " + userId);
        CommentResponseDTO response = commentService.addComment(videoId, userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{videoId}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable Long videoId) {
        System.out.println("Received request to get comments for videoId: " + videoId);
        return ResponseEntity.ok(commentService.getCommentsForVideo(videoId));
    }


}