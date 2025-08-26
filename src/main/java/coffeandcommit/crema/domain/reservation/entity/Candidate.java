package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.domain.guide.entity.Time;
import coffeandcommit.crema.global.common.entitiy.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


// 후보 저장 테이블
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "candidate")
public class Candidate extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservationId; // 예약 FK ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_id", nullable = false)
    private Time timeId; // 시간 FK ID

    @Column(nullable = false)
    private int priority; // 우선순위

    @Column(nullable = false)
    private LocalDateTime date; // 날짜

}
