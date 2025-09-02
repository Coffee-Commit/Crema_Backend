package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuideJobFieldRepository extends JpaRepository<GuideJobField, Long> {

    Optional<GuideJobField> findByGuide(Guide guide);
}


