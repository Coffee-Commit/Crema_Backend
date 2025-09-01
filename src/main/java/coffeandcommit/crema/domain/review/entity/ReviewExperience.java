package coffeandcommit.crema.domain.review.entity;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "review_experience",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_review_experience_review_group",
            columnNames = {"review_id", "experience_group_id"}
        )
    },
    indexes = {
        @Index(name = "idx_review_experience_review_id", columnList = "review_id"),
        @Index(name = "idx_review_experience_experience_group_id", columnList = "experience_group_id")
    }
)
public class ReviewExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review; // FK, 후기 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experience_group_id", nullable = false)
    private ExperienceGroup experienceGroup; // FK, 경험 대주제 ID

    @Column(name = "thumbs_up", nullable = false)
    private boolean thumbsUp; // 좋아요 여부
}
