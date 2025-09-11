package coffeandcommit.crema.domain.videocall.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "화상통화 참가자 정보 응답")
public class ParticipantInfoResponse {
    
    @Schema(description = "참가자 닉네임", example = "개발자김철수")
    private String participantName;
    
    @Schema(description = "화상통화 분야", example = "백엔드")
    private String videoChatField;
    
    @Schema(description = "화상통화 주제", example = "Spring Boot 기초")
    private String videoChatTopic;
}