package coffeandcommit.crema.domain.videocall.repository;

import coffeandcommit.crema.domain.videocall.entity.VideoCallSharedFile;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoCallSharedFileRepository extends JpaRepository<VideoCallSharedFile, Long> {
    
    /**
     * 특정 세션의 모든 공유 파일 조회 (업로드 시간 기준 내림차순)
     */
    List<VideoCallSharedFile> findByVideoSessionOrderByUploadedAtDesc(VideoSession videoSession);
    
    /**
     * 특정 세션과 이미지 키로 공유 파일 존재 여부 확인
     */
    boolean existsByVideoSessionAndImageKey(VideoSession videoSession, String imageKey);
    
    /**
     * 특정 세션과 이미지 키로 공유 파일 조회
     */
    Optional<VideoCallSharedFile> findByVideoSessionAndImageKey(VideoSession videoSession, String imageKey);
    
    /**
     * 특정 세션의 공유 파일 개수 조회
     */
    long countByVideoSession(VideoSession videoSession);
    
    /**
     * 특정 사용자가 업로드한 파일들 조회
     */
    List<VideoCallSharedFile> findByVideoSessionAndUploadedByUserIdOrderByUploadedAtDesc(
        VideoSession videoSession, String uploadedByUserId);
    
    /**
     * 세션 ID로 공유 파일 조회 (VideoSession을 조인하여 효율적 조회)
     */
    @Query("SELECT vsf FROM VideoCallSharedFile vsf " +
           "JOIN FETCH vsf.videoSession vs " +
           "WHERE vs.sessionId = :sessionId " +
           "ORDER BY vsf.uploadedAt DESC")
    List<VideoCallSharedFile> findBySessionIdOrderByUploadedAtDesc(@Param("sessionId") String sessionId);
    
    /**
     * 세션 ID와 이미지 키로 공유 파일 존재 여부 확인
     */
    @Query("SELECT COUNT(vsf) > 0 FROM VideoCallSharedFile vsf " +
           "WHERE vsf.videoSession.sessionId = :sessionId AND vsf.imageKey = :imageKey")
    boolean existsBySessionIdAndImageKey(@Param("sessionId") String sessionId, @Param("imageKey") String imageKey);
    
    /**
     * 특정 이미지 키를 가진 모든 공유 파일 삭제 (S3 파일 삭제 시 사용)
     */
    void deleteByImageKey(String imageKey);
}