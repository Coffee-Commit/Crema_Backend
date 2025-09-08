package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.member.entity.MemberChatTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "멤버 커피챗 주제 응답")
public class MemberChatTopicResponse {

    @Schema(description = "멤버 커피챗 주제 ID", example = "1")
    private Long id;

    @Schema(description = "멤버 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String memberId;

    @Schema(description = "커피챗 주제 정보")
    private TopicDTO topic;

    public static MemberChatTopicResponse from(MemberChatTopic memberChatTopic) {
        TopicDTO topic = TopicDTO.builder()
                .topicName(memberChatTopic.getChatTopic().getTopicName())
                .build();

        return MemberChatTopicResponse.builder()
                .id(memberChatTopic.getId())
                .memberId(memberChatTopic.getMember().getId())
                .topic(topic)
                .build();
    }
}