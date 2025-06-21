package io.muzoo.scalable.vms.CommentUtils;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    // For retrieving comments on a video in chronological order
    List<VideoComment> findByVideoIdOrderByCreatedAtAsc(Long videoId);
}