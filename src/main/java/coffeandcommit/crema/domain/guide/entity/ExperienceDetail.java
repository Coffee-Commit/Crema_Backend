package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "experience_detail")
public class ExperienceDetail extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_group_id", nullable = false)
    private ExperienceGroup groupId; // FK, 가이드 ID

    @Column(nullable = false)
    private String who;

    @Column(nullable = false)
    private String solution;

    @Column(nullable = false)
    private String how;
}
