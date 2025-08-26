package coffeandcommit.crema.domain.member.entity;

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
    name = "member_job_field",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"member_id", "job_field_id"}
        )
    },
    indexes = {
        @Index(columnList = "member_id"),
        @Index(columnList = "job_field_id")
    }
)
public class MemberJobField extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member memberId; // FK, 멤버 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_field_id", nullable = false)
    private JobField jobFieldId; // FK, 직무 분야 ID
}
