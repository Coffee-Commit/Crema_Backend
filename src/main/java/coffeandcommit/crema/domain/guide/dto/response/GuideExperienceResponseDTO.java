package coffeandcommit.crema.domain.guide.dto.response;

import coffeandcommit.crema.domain.guide.entity.ExperienceGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideExperienceResponseDTO {

    private List<GroupResponseDTO> groups;

    public static GuideExperienceResponseDTO from(List<ExperienceGroup> experienceGroups) {

        List<GroupResponseDTO> mapped = (experienceGroups == null) ? List.of() :
                experienceGroups.stream()
                        .map(GroupResponseDTO::from)
                        .toList();

        return GuideExperienceResponseDTO.builder()
                .groups(mapped)
                .build();
    }
}
