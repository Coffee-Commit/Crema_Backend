package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.enums.Status;
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
    private Status status = Status.PENDING; // 예약 상태 (예: PENDING, CONFIRMED, COMPLETED)

    private String reason;

    private LocalDateTime reservedAt;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private TimeUnit timeUnit;

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        if (timeUnit.getReservation() != this) {
            timeUnit.setReservation(this); // 반대편도 세팅
        }
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }



}
