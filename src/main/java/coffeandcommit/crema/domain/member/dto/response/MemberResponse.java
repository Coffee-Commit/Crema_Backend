package coffeandcommit.crema.domain.member.dto.response;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MemberResponse {
    private String id;
    private String nickname;
    private MemberRole role;
    private String email;
    private Integer point;
    private String profileImageUrl;
    private String description;
    private String provider;
    private LocalDateTime createdAt;
}