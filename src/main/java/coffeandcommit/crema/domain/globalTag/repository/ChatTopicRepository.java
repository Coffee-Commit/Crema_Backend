package coffeandcommit.crema.domain.globalTag.repository;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatTopicRepository extends JpaRepository<ChatTopic, Long> {


    Optional<ChatTopic> findByTopicName(TopicNameType topicName);
}
