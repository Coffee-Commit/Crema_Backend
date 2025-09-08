package coffeandcommit.crema.domain.reservation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SurveyRequestDTO {

    @NotBlank(message = "가이드에게 보낼 메시지는 필수입니다.")
    private String messageToGuide; // 사전 메시지

    @NotNull(message = "희망 날짜는 필수입니다.")
    private LocalDateTime preferredDate; // 희망 날짜

    @Valid
    @Builder.Default
    private List<@Valid SurveyFileRequestDTO> files = new ArrayList<>(); // 여러 개 파일 업로드
}
