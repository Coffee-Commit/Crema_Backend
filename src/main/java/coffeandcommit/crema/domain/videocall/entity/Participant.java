package coffeandcommit.crema.domain.videocall.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", unique = true, nullable = false)
    private String connectionId;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_connected")
    private Boolean isConnected;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_session_id")
    private VideoSession videoSession;

    @Builder
    public Participant(String connectionId, String token, String username, VideoSession videoSession) {
        this.connectionId = connectionId;
        this.token = token;
        this.username = username;
        this.videoSession = videoSession;
        this.joinedAt = LocalDateTime.now();
        this.isConnected = true;
    }

    public void leaveSession() {
        this.isConnected = false;
        this.leftAt = LocalDateTime.now();
    }

    public void setVideoSession(VideoSession videoSession) {
        this.videoSession = videoSession;
    }
}