package coffeandcommit.crema.domain.reservation.entity;

import coffeandcommit.crema.domain.guide.entity.TimeSlot;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


// 후보 저장 테이블
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "candidate",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"reservation_id", "time_id", "candidate_date"})
       },
       indexes = {
           @Index(columnList = "reservation_id"),
           @Index(columnList = "reservation_id,candidate_date")
})
public class Candidate extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation; // 예약 FK ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private TimeSlot timeSlot; // 시간 FK ID

    @Column(nullable = false)
    private int priority; // 우선순위

    @Column(name = "candidate_date", nullable = false)
    private LocalDateTime date; // 날짜

}
