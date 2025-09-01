package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideChatTopicResponseDTO {

    private Long id; // GuideChatTopic id
    private Long guideId;
    private TopicDTO topic;

    public static GuideChatTopicResponseDTO from(GuideChatTopic guideChatTopic) {
        TopicDTO topic = TopicDTO.builder()
                .chatTopic(guideChatTopic.getChatTopic().getChatTopic())
                .topicName(guideChatTopic.getChatTopic().getTopicName())
                .build();

        return GuideChatTopicResponseDTO.builder()
                .id(guideChatTopic.getId())
                .guideId(guideChatTopic.getGuide().getId())
                .topic(topic)
                .build();
    }
}
