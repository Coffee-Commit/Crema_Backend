package coffeandcommit.crema.domain.member.entity;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "member_job_field",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "job_name"})
        },
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_member_job_name", columnList = "job_name")
        }
)
public class MemberJobField extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_name", nullable = false)
    private JobNameType jobName; // 직무 분야 이름
}
