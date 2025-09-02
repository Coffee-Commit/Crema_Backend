package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.HashTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface HashTagRepository extends JpaRepository<HashTag, Long> {
    long countByGuide(Guide guide);

    boolean existsByGuideAndHashTagName(Guide guide, String hashTagName);

    List<HashTag> findByGuide(Guide guide);
}
