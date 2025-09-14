package coffeandcommit.crema.domain.videocall.entity;

import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_session_id")
    private Long id;

    @Column(unique = true)
    private String sessionName;

    private String sessionId;

    private String sessionToken;

    private LocalDateTime endedAt;

    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "videoSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
        participant.setVideoSession(this);
    }

    public void endSession() {
        this.isActive = false;
        this.endedAt = LocalDateTime.now();
    }

    /**
     * 세션을 활성 상태로 설정 (재생성 시 사용)
     */
    public void activateSession() {
        this.isActive = true;
        this.endedAt = null;
    }

    /**
     * 세션을 비활성 상태로 설정
     */
    public void deactivateSession() {
        this.isActive = false;
        this.endedAt = LocalDateTime.now();
    }
}
