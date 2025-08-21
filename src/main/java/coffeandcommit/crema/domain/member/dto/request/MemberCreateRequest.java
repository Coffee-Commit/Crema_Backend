package coffeandcommit.crema.domain.member.dto.request;

import coffeandcommit.crema.domain.member.entity.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberCreateRequest {

    @NotBlank(message = "아이디는 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 최소 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;

    @NotBlank(message = "실명은 필수입니다")
    private String realName;

    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;

    @Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$",
            message = "올바른 휴대폰 번호 형식이어야 합니다")
    private String phoneNumber;

    private String profileImageUrl;
    private String description;

    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .username(username)
                .password(encodedPassword)
                .realName(realName)
                .nickname(nickname)
                .phoneNumber(phoneNumber)
                .profileImageUrl(profileImageUrl)
                .description(description)
                .build();
    }
}