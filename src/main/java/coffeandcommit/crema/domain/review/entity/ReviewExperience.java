package coffeandcommit.crema.domain.review.entity;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "review_experience")
public class ReviewExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review reviewId; // FK, 후기 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_group_id", nullable = false)
    private ExperienceGroup experienceGroupId; // FK, 경험 대주제 ID

    @Column(nullable = false)
    private boolean thumbsUp; // 좋아요 여부
}
