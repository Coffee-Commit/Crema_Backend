package coffeandcommit.crema.domain.videocall.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "video_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    @Column(name = "session_name", nullable = false)
    private String sessionName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "videoSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Participant> participants = new ArrayList<>();

    @Builder
    public VideoSession(String sessionId, String sessionName) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void endSession() {
        this.isActive = false;
        this.endedAt = LocalDateTime.now();
    }

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
        participant.setVideoSession(this);
    }
}