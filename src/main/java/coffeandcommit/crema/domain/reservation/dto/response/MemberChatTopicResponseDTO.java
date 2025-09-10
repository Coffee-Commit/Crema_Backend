package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.member.entity.MemberChatTopic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberChatTopicResponseDTO {
    private Long id;          // MemberChatTopic ID
    private String memberId;  // 멘티 Member UUID
    private TopicDTO topic;   // 글로벌 태그 DTO 재사용

    public static MemberChatTopicResponseDTO from(MemberChatTopic memberChatTopic) {
        TopicDTO topic = TopicDTO.from(memberChatTopic.getChatTopic().getTopicName());

        return MemberChatTopicResponseDTO.builder()
                .id(memberChatTopic.getId())
                .memberId(memberChatTopic.getMember().getId())
                .topic(topic)
                .build();
    }
}
