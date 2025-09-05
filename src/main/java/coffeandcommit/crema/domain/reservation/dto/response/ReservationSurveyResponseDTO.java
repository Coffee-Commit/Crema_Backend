package coffeandcommit.crema.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationSurveyResponseDTO {

    private String messageToGuide;
    @Builder.Default
    private List<SurveyFileResponseDTO> files = Collections.emptyList();
    private MemberDTO member;
    private GuideSurveyResponseDTO guide;
}
