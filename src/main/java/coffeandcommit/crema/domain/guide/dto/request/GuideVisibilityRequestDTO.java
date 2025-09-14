package coffeandcommit.crema.domain.guide.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class GuideVisibilityRequestDTO {

    @JsonProperty("isOpened")
    private boolean isOpened;
}
