package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.guide.enums.DayType;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "time_slot",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_time_slot__day_start_end",
            columnNames = {"day", "start_time_option", "end_time_option"}
        )
    },
    indexes = {
        @Index(name = "idx_time_slot__day", columnList = "day")
    }
)
public class TimeSlot extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time_option", nullable = false)
    private LocalTime startTimeOption; // 시작 시간

    @Column(name = "end_time_option", nullable = false)
    private LocalTime endTimeOption; // 종료 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false)
    private DayType day;
}
