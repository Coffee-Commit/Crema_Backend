package coffeandcommit.crema.domain.videocall.repository;

import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import java.util.Optional;

@Repository
public interface VideoSessionRepository extends JpaRepository<VideoSession, Long> {
    Optional<VideoSession> findBySessionId(String sessionId);
    Optional<VideoSession> findBySessionIdAndIsActiveTrue(String sessionId);
    Optional<VideoSession> findBySessionNameAndIsActiveTrue(String sessionName);
    
    /**
     * 동시성 안전을 위한 행 잠금으로 세션 조회
     * OpenVidu 세션 재생성 시 중복 생성 방지용
     * @param sessionId 세션 ID
     * @return 잠긴 VideoSession 엔티티
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")) // 3초 타임아웃
    @Query("select vs from VideoSession vs where vs.sessionId = :sessionId")
    Optional<VideoSession> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}
