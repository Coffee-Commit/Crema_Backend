package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.reservation.entity.SurveyFile;
import coffeandcommit.crema.global.storage.StorageService;
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
    private String fileUrl;

    public static SurveyFileResponseDTO from(SurveyFile file, StorageService storageService) {
        return SurveyFileResponseDTO.builder()
                .id(file.getId())
                .fileUrl(storageService.generateViewUrl(file.getFileKey()))
                .build();
    }
}
