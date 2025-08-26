package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "guide_schedule",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"guide_id", "time_id"})
    },
    indexes = {
        @Index(name = "idx_guide_schedule_guide", columnList = "guide_id"),
        @Index(name = "idx_guide_schedule_time", columnList = "time_id")
    }
)
public class GuideSchedule extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guideId; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_id", nullable = false)
    private TimeSlot timeId; // FK, 시간 ID
}
