package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperienceGroupRepository extends JpaRepository<ExperienceGroup, Long> {
    long countByGuide(Guide guide);

    @EntityGraph(attributePaths = {"guide", "guideChatTopic", "guideChatTopic.chatTopic"})
    List<ExperienceGroup> findByGuide(Guide guide);

    Optional<ExperienceGroup> findByGuideChatTopic(GuideChatTopic guideChatTopic);
}
