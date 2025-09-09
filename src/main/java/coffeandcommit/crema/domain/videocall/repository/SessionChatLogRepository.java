package coffeandcommit.crema.domain.videocall.repository;

import coffeandcommit.crema.domain.videocall.entity.SessionChatLog;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionChatLogRepository extends JpaRepository<SessionChatLog, Long> {

    Optional<SessionChatLog> findBySessionId(String sessionId);
    
    Optional<SessionChatLog> findByVideoSession(VideoSession videoSession);
    
    boolean existsBySessionId(String sessionId);
}