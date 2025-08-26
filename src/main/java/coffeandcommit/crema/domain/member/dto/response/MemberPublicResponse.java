package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 타인 조회용 회원 정보 응답 DTO
 * 개인정보 (phoneNumber, point, provider, createdAt) 제외
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MemberPublicResponse {
    private String id;
    private String nickname;
    private MemberRole role;
    private String profileImageUrl;
    private String description;
}