package coffeandcommit.crema.domain.guide.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GuideHashTagRequestDTO {

    @Size(max = 8, message = "해시태그는 최대 8글자까지 가능합니다.") // 한글 기준 8글자 제한
    private String hashTagName;

}
