package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MemberResponse {
    private String id;
    private String username;
    private String realName;
    private String nickname;
    private MemberRole role;
    private String phoneNumber;
    private Integer points;
    private String profileImageUrl;
    private String introduction;
    private String provider;
}


