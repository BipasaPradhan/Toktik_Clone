package io.muzoo.scalable.vms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByObjectKey(String objectKey);

    @Query(value = "SELECT * FROM vms_video_data WHERE visibility = :visibility AND status = 'UPLOADED' ORDER BY upload_time DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Video> findByVisibilityAndStatus(String visibility, int limit, int offset);
}