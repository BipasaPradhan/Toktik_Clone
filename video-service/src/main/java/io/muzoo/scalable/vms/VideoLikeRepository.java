package io.muzoo.scalable.vms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {
    Optional<VideoLike> findByVideoIdAndUserId(Long videoId, String userId);

}