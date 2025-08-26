package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.globalTag.entity.JobField;
import coffeandcommit.crema.global.common.entitiy.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "guide_job_field")
public class GuideJobField extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guideId; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_field_id", nullable = false)
    private JobField jobFieldId; // FK, 직무 분야 ID
}
