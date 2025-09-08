package coffeandcommit.crema.domain.videocall.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "session_chat_log", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_session_chat_log_session_id", columnNames = "session_id")
       },
       indexes = {
           @Index(name = "idx_session_chat_log_session_id", columnList = "session_id"),
           @Index(name = "idx_session_chat_log_created_at", columnList = "created_at")
       })
public class SessionChatLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_chat_log_id")
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_messages", columnDefinition = "JSON")
    private String chatMessages;

    @Column(name = "total_messages")
    private Integer totalMessages;

    @Column(name = "session_start_time")
    private LocalDateTime sessionStartTime;

    @Column(name = "session_end_time")
    private LocalDateTime sessionEndTime;

    @Column(name = "saved_by", length = 50)
    private String savedBy;
    @Version
    @Column(name = "version")
    private Long version;

    @OneToOne
    @JoinColumn(name = "video_session_id")
    private VideoSession videoSession;

    public void updateChatHistory(String chatMessages, Integer totalMessages, LocalDateTime sessionEndTime) {
        this.chatMessages = chatMessages;
        this.totalMessages = totalMessages;
        this.sessionEndTime = sessionEndTime;
    }

    public void updateChatHistoryWithMetadata(String chatMessages, Integer totalMessages, 
                                             LocalDateTime sessionEndTime, String savedBy) {
        this.chatMessages = chatMessages;
        this.totalMessages = totalMessages;
        this.sessionEndTime = sessionEndTime;
        this.savedBy = savedBy;
    }
}