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
                name = "uk_time_slot__schedule_start_end",
                columnNames = {"schedule_id", "start_time_option", "end_time_option"}
        )
    },
    indexes = {
        @Index(name = "idx_time_slot__schedule", columnList = "schedule_id")
    }
)
public class TimeSlot extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private GuideSchedule schedule; // FK, 가이드 스케줄 ID

    @Column(name = "start_time_option", nullable = false)
    private LocalTime startTimeOption; // 시작 시간

    @Column(name = "end_time_option", nullable = false)
    private LocalTime endTimeOption; // 종료 시간

    public void setSchedule(GuideSchedule schedule) {
        this.schedule = schedule;
    }

}
