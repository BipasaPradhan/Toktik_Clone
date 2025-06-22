package io.muzoo.scalable.vms;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByObjectKey(String objectKey);

    @Query(value = "SELECT * FROM vms_video_data WHERE visibility = :visibility AND status = 'UPLOADED' ORDER BY upload_time DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Video> findByVisibilityAndStatus(String visibility, int limit, int offset);

    @Query(value = "SELECT * FROM vms_video_data WHERE user_id = :userId AND status IN ('PROCESSING', 'UPLOADED', 'READY') ORDER BY upload_time DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Video> findByUserIdAndStatus(String userId, int limit, int offset);

    @Query(value = "SELECT * FROM vms_video_data WHERE id = :id", nativeQuery = true)
    Optional<Video> findByIdNative(Long id);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + :delta WHERE v.id = :videoId")
    void incrementViewCountByDelta(@Param("videoId") Long videoId, @Param("delta") Long delta);


    @Modifying
    @Query("UPDATE Video v SET v.viewCount = :viewCount WHERE v.id = :videoId")
    void updateViewCount(@Param("videoId") Long videoId, @Param("viewCount") Long viewCount);

    Optional<Video> findByIdAndUserId(Long id, String userId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + 1 WHERE v.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    // Ensure likeCount doesn't go negative
    @Modifying
    @Query("UPDATE Video v SET v.likeCount = CASE WHEN v.likeCount - 1 < 0 THEN 0 ELSE v.likeCount - 1 END WHERE v.id = :id")
    void decrementLikeCountSafely(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = :likeCount WHERE v.id = :videoId")
    void updateLikeCount(@Param("videoId") Long videoId, @Param("likeCount") Long likeCount);
}