package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.entity.Survey;
import coffeandcommit.crema.domain.videocall.entity.VideoSession;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;


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

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "survey_id", nullable = false, unique = true)
    private Survey survey; // FK, 설문 ID

    private LocalDateTime matchingTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING; // 예약 상태 (예: PENDING, CONFIRMED, COMPLETED)

    private LocalDateTime reservedAt;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private TimeUnit timeUnit;

    public void setTimeUnit(TimeUnit timeUnit) {
        if (Objects.equals(this.timeUnit, timeUnit)) return;

        if (timeUnit == null) {
            throw new BaseException(ErrorStatus.INVALID_TIME_UNIT);
        }

        this.timeUnit = timeUnit;

        if (timeUnit.getReservation() != this) {
            timeUnit.setReservation(this);
        }
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_session_id")
    private VideoSession videoSession;

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

    public void confirmReservation() {
        this.status = Status.CONFIRMED;
    }

    public void cancelReservation(String reason) {
        this.status = Status.CANCELLED;
    }
}
