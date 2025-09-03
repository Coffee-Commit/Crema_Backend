package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceGroupRepository extends JpaRepository<ExperienceGroup, Long> {
    long countByGuide(Guide guide);

    List<ExperienceGroup> findByGuide(Guide guide);
}
