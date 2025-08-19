package coffeandcommit.crema.domain.videocall.repository;

import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoSessionRepository extends JpaRepository<VideoSession, Long> {
    Optional<VideoSession> findBySessionId(String sessionId);
    Optional<VideoSession> findBySessionIdAndIsActiveTrue(String sessionId);
    
    // 고급 API를 위한 추가 메서드
    Optional<VideoSession> findBySessionNameAndIsActiveTrue(String sessionName);
}