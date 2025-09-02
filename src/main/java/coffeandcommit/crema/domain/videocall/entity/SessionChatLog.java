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
@Table(name = "session_chat_log")
public class SessionChatLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_chat_log_id")
    private Long id;

    @Column(name = "session_id", nullable = false)
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

    @OneToOne
    @JoinColumn(name = "video_session_id")
    private VideoSession videoSession;

    public void updateChatHistory(String chatMessages, Integer totalMessages, LocalDateTime sessionEndTime) {
        this.chatMessages = chatMessages;
        this.totalMessages = totalMessages;
        this.sessionEndTime = sessionEndTime;
    }
}