package coffeandcommit.crema.domain.guide.repository;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideChatTopicRepository extends JpaRepository<GuideChatTopic, Long> {
    boolean existsByGuideAndChatTopic(Guide guide, ChatTopic chatTopic);
    java.util.Optional<GuideChatTopic> findByGuideAndChatTopic(Guide guide, ChatTopic chatTopic);


    @Query("SELECT gct FROM GuideChatTopic gct " +
            "JOIN FETCH gct.chatTopic ct " +
            "JOIN FETCH gct.guide g " +
            "WHERE g = :guide")
    List<GuideChatTopic> findAllByGuideWithJoin(@Param("guide") Guide guide);

    long countByGuide(Guide guide);

}
