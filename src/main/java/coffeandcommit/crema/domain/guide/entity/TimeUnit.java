package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "time_unit",
    indexes = {
        @Index(columnList = "reservation_id"),
        @Index(columnList = "time_type")
    }
)
public class TimeUnit extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_type", nullable = false)
    private TimeType timeType;

    public void setReservation(Reservation reservation) {

        // 동일 객체 재설정 방지
        if (Objects.equals(this.reservation, reservation)) return;

        // null 방어
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        this.reservation = reservation;

        // 양방향 연관관계 설정
        if (reservation.getTimeUnit() != this) {
            reservation.setTimeUnit(this);
        }
    }

}
