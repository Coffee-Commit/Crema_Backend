package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.guide.enums.DayType;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "guide_schedule",
    indexes = {
        @Index(name = "idx_guide_schedule__guide", columnList = "guide_id"),
        @Index(name = "idx_guide_schedule__day", columnList = "day")
    }
)
public class GuideSchedule extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide; // FK, 가이드 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false, length = 20)
    private DayType day;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSlot> timeSlots = new ArrayList<>();

    // 연관관계 메서드
    public void addTimeSlot(TimeSlot slot) {
        timeSlots.add(slot);
        slot.setSchedule(this);
    }

    public void removeTimeSlot(TimeSlot slot) {
        timeSlots.remove(slot);
        slot.setSchedule(null);
    }
}
