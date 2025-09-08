package coffeandcommit.crema.domain.review.repository;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.review.entity.ReviewExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewExperienceRepository extends JpaRepository<ReviewExperience, Long> {
    @Query("""
            SELECT COUNT(re)
            FROM ReviewExperience re
            WHERE re.experienceGroup.guide = :guide
              AND re.isThumbsUp = true
    """)
    Long countThumbsUpByGuide(@Param("guide") Guide guide);

    Long countByExperienceGroup(ExperienceGroup experienceGroup);

    Long countByExperienceGroupAndIsThumbsUpTrue(ExperienceGroup experienceGroup);
}
