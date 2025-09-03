package coffeandcommit.crema.domain.guide.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideExperienceRequestDTO {

    @NotNull(message = "경험 대주제 목록은 필수입니다.")
    private List<GroupRequestDTO> groups;
}
