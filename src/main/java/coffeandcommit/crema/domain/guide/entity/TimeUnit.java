package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.guide.enums.TimeType;
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
            columnNames = {"guide_id", "time_type"}
        )
    },
    indexes = {
        @Index(columnList = "guide_id"),
        @Index(columnList = "time_type")
    }
)
public class TimeUnit extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guideId; // FK, 가이드 ID

    @Enumerated(EnumType.STRING)
    private TimeType timeType; // 30분, 60분

    @Column
    private int price; // 가격

}
