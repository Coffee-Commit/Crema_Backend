package coffeandcommit.crema.domain.member.dto.request;

import coffeandcommit.crema.domain.globalTag.enums.TopicNameType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "멤버 커피챗 주제 등록/수정 요청")
public class MemberChatTopicRequest {

    @Schema(description = "관심 커피챗 주제 목록", example = "[\"RESUME\", \"INTERVIEW\", \"PRACTICAL_WORK\"]")
    @Size(max = 10, message = "관심 주제는 최대 10개까지 선택할 수 있습니다")
    private List<TopicNameType> topicNames;
}