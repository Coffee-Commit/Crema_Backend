package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.reservation.entity.SurveyFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SurveyFileResponseDTO {

    private Long id;
    private String fileUploadUrl;

    public static SurveyFileResponseDTO from(SurveyFile file) {
        return SurveyFileResponseDTO.builder()
                .id(file.getId())
                .fileUploadUrl(file.getFileUploadUrl())
                .build();
    }
}
