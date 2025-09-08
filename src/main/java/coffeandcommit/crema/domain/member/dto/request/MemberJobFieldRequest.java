package coffeandcommit.crema.domain.member.dto.request;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "멤버 커피챗 분야 등록/수정 요청")
public class MemberJobFieldRequest {

    @Schema(description = "관심 커피챗 분야", example = "IT_DEVELOPMENT_DATA")
    private JobNameType jobName;
}