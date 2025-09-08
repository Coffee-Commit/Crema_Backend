package coffeandcommit.crema.domain.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideCoffeeChatRequestDTO {

    @NotBlank(message = "커피챗 제목은 필수 입력 값입니다.")
    @Size(max = 200, message = "커피챗 제목은 최대 200자까지 가능합니다.")
    private String title;

    @NotBlank(message = "커피챗 소개글은 필수 입력 값입니다.")
    @Size(max = 1000, message = "커피챗 소개글은 최대 1000자까지 가능합니다.")
    private String chatDescription;
}
