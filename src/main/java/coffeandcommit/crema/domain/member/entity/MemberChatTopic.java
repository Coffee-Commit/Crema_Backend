package coffeandcommit.crema.domain.member.entity;

import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
    name = "member_chat_topic",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"member_id", "chat_topic_id"}
        )
    },
    indexes = {
        @Index(columnList = "member_id"),
        @Index(columnList = "chat_topic_id")
    }
)
public class MemberChatTopic extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member; // FK, 멤버 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_topic_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChatTopic chatTopic; // FK, 주제 ID
}
