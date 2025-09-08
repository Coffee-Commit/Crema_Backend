package coffeandcommit.crema.domain.reservation.dto.response;

import coffeandcommit.crema.domain.reservation.entity.Survey;
import coffeandcommit.crema.global.storage.StorageService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SurveyResponseDTO {

    private Long id;
    private String messageToGuide;
    private LocalDateTime preferredDate;
    private List<SurveyFileResponseDTO> files;

    public static SurveyResponseDTO from(Survey survey, StorageService storageService) {
        return SurveyResponseDTO.builder()
                .id(survey.getId())
                .messageToGuide(survey.getMessageToGuide())
                .preferredDate(survey.getPreferredDate())
                .files(
                        survey.getFiles().stream()
                                .map(file -> SurveyFileResponseDTO.from(file, storageService))
                                .collect(Collectors.toList())
                )
                .build();
    }
}
