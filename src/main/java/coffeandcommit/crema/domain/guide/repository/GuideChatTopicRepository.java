package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideChatTopicRepository extends JpaRepository<GuideChatTopic, Long> {
    boolean existsByGuideAndChatTopic_TopicName(Guide guide, TopicNameType chatTopicTopicName);

    List<GuideChatTopic> findAllByGuide(Guide guide);
}
