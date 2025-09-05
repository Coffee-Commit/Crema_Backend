package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "guide_job_field",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"guide_id", "job_name"})
        },
        indexes = {
                @Index(name = "idx_guide_id", columnList = "guide_id"),
                @Index(name = "idx_guide_job_name", columnList = "job_name")
        }
)
public class GuideJobField extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false, unique = true)
    private Guide guide;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_name", nullable = false)
    private JobNameType jobName; // 직무 분야 이름

    public void updateJobName(JobNameType jobName) {
        if (jobName == null) {
            throw new IllegalArgumentException("직무 분야는 null일 수 없습니다.");
        }
        this.jobName = jobName;
    }
}
