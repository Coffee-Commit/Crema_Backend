package coffeandcommit.crema.domain.reservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationSurveyResponseDTO {

    private String messageToGuide;
    private List<SurveyFileResponseDTO> files;
    private MemberDTO member;
    private GuideSurveyResponseDTO guide;
}
