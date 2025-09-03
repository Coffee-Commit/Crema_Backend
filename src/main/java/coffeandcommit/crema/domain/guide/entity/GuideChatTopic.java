package coffeandcommit.crema.domain.guide.entity;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "guide_chat_topic",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"guide_id", "chat_topic_id"})
    },
    indexes = {
        @Index(name = "idx_gct_guide", columnList = "guide_id"),
        @Index(name = "idx_gct_topic", columnList = "chat_topic_id")
    }
)
public class GuideChatTopic extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide; // FK, 가이드 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_topic_id", nullable = false)
    private ChatTopic chatTopic; // FK, 챗 토픽 ID

    @OneToOne(mappedBy = "guideChatTopic", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private ExperienceGroup experienceGroup;
}
