package coffeandcommit.crema.domain.meeting.entity;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "meeting_room")
public class MeetingRoom extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservationId; // FK, 예약 ID

    @Column(nullable = false)
    private LocalDateTime startTime; // 시작 시간

    @Column(nullable = false)
    private LocalDateTime endTime; // 종료 시간

    @Column(nullable = false)
    private String roomUrl; // 회의실 URL
}
