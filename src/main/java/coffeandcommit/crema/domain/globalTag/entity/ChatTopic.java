package coffeandcommit.crema.domain.globalTag.entity;

import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "chat_topic",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"chat_topic", "topic_name"}) // 대분류+소분류 조합 유니크
        },
        indexes = {
                @Index(name = "idx_chat_topic", columnList = "chat_topic"),
                @Index(name = "idx_topic_name", columnList = "topic_name")
        }
)
public class ChatTopic extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_topic",nullable = false)
    private ChatTopicType chatTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_name",nullable = false)
    private TopicNameType topicName; // 서브 주제 이름

}
