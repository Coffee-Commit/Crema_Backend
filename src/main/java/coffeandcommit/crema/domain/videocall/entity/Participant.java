package coffeandcommit.crema.domain.videocall.entity;

import coffeandcommit.crema.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", unique = true, nullable = false)
    private String connectionId;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "username", nullable = false, length = 50)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void leaveSession() {
        this.isConnected = false;
        this.leftAt = LocalDateTime.now();
    }

    public void setVideoSession(VideoSession videoSession) {
        this.videoSession = videoSession;
    }
}