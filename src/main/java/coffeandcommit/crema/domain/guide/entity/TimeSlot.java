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
@Table(name = "time_slot")
public class TimeSlot extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalTime startTimeOption; // 시작 시간

    @Column(nullable = false)
    private LocalTime endTimeOption; // 종료 시간

    @Enumerated(EnumType.STRING)
    private DayType day;
}
