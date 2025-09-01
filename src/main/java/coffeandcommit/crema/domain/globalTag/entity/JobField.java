package coffeandcommit.crema.domain.globalTag.entity;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.enums.JobType;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "job_field",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"job_type", "job_name"})
        },
        indexes = {
                @Index(name = "idx_job_type", columnList = "job_type"),
                @Index(name = "idx_job_name", columnList = "job_name")
        }
)
public class JobField extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType; // 직무 대분류 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "job_name", nullable = false)
    private JobNameType jobName; // 직무 분야 이름
}
