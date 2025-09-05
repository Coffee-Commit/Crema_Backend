package coffeandcommit.crema.domain.globalTag.entity;

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
        indexes = {
                @Index(name = "idx_topic_name", columnList = "topic_name")
        }
)
public class ChatTopic extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_name",nullable = false)
    @Builder.Default
    private TopicNameType topicName = TopicNameType.UNDEFINED;; // 서브 주제 이름

}
