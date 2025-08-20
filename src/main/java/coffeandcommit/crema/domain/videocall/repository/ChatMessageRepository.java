package coffeandcommit.crema.domain.videocall.repository;

import coffeandcommit.crema.domain.videocall.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = :sessionId ORDER BY c.createdAt ASC")
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") String sessionId);
    
    @Query("SELECT c FROM ChatMessage c WHERE c.sessionId = :sessionId ORDER BY c.createdAt DESC")
    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") String sessionId);
    
    @Query("SELECT c FROM ChatMessage c ORDER BY c.createdAt DESC")
    List<ChatMessage> findAllByOrderByCreatedAtDesc();
}