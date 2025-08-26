package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.survey.entity.Survey;
import coffeandcommit.crema.global.common.entitiy.BaseEntity;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guideId; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member memberId; // FK, 멤버 ID는 String (UUID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey surveyId; // FK, 설문 ID

    private LocalDateTime matchingTime;

    @Enumerated(EnumType.STRING)
    private Status status; // 예약 상태 (예: PENDING, CONFIRMED, CANCELLED 등)

    private String reason;

    private LocalDateTime reservedAt;
}
