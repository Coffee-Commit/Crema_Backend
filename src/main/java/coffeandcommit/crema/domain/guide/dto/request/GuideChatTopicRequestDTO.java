package coffeandcommit.crema.domain.guide.dto.request;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideChatTopicRequestDTO {

    @Valid
    @NotNull
    private List<TopicDTO> topics;
}
