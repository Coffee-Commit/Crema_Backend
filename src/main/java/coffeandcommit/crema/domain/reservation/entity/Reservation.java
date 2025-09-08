package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.survey.entity.Survey;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "reservation")
public class Reservation extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // FK, 멤버 ID는 String (UUID)

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false, unique = true)
    private Survey survey; // FK, 설문 ID

    private LocalDateTime matchingTime;

    @Enumerated(EnumType.STRING)
    private Status status; // 예약 상태 (예: PENDING, CONFIRMED, COMPLETED)

    private String reason;

    private LocalDateTime reservedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_session_id")
    private VideoSession videoSession;
    /**
 * 예약 상태를 완료로 변경 (멱등성 보장)
 */
public void completeReservation() {
    if (this.status == Status.COMPLETED) {
        // 이미 완료된 경우 - 멱등성
        return;
    }
    
    if (this.status == Status.CANCELLED) {
        throw new IllegalStateException("취소된 예약은 완료할 수 없습니다.");
    }
    
    this.status = Status.COMPLETED;
}
    
    /**
     * 예약 상태를 확정으로 변경
     */
    public void confirmReservation() {
        this.status = Status.CONFIRMED;
    }
    
    /**
     * 예약 상태를 취소로 변경
     */
    public void cancelReservation(String reason) {
        this.status = Status.CANCELLED;
        this.reason = reason;
    }
}
