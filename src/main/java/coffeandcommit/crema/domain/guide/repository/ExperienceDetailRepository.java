package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.ExperienceDetail;
import coffeandcommit.crema.domain.guide.entity.Guide;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ExperienceDetailRepository extends JpaRepository<ExperienceDetail, Long> {

    @EntityGraph(attributePaths = "guide")
    Optional<ExperienceDetail> findByGuide(Guide guide);
}
