package coffeandcommit.crema.domain.globalTag.entity;

import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.global.common.entitiy.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "chat_topic")
public class ChatTopic extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private ChatTopicType chatTopic;

    @Column(nullable = false)
    private TopicNameType topicName; // 서브 주제 이름

}
