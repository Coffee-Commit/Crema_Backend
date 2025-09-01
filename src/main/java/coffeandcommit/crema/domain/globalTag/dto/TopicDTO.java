package coffeandcommit.crema.domain.globalTag.dto;

import coffeandcommit.crema.domain.globalTag.enums.ChatTopicType;
import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicDTO {

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ChatTopicType chatTopic;
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private TopicNameType topicName;
}
