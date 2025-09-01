package coffeandcommit.crema.domain.guide.dto.request;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideChatTopicRequestDTO {

    private List<TopicDTO> topics;
}
