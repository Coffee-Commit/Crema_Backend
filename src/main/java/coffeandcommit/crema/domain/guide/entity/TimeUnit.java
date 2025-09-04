package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "time_unit",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"reservation_id", "time_type"} // 예약별로 30분/60분 중 하나만
        )
    },
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
        this.reservation = reservation;
        if (reservation.getTimeUnit() != this) {
            reservation.setTimeUnit(this);
        }
    }

}
