package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.globalTag.entity.JobField;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "guide_job_field",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_guide_job_field_pair",
            columnNames = {"guide_id", "job_field_id"}
        )
    },
    indexes = {
        @Index(name = "idx_guide_job_field_guide", columnList = "guide_id"),
        @Index(name = "idx_guide_job_field_job_field", columnList = "job_field_id")
    }
)
public class GuideJobField extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guideId; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_field_id", nullable = false)
    private JobField jobFieldId; // FK, 직무 분야 ID
}
