package coffeandcommit.crema.global.auth.provider;

import jakarta.validation.constraints.NotNull;

public interface OAuth2UserInfo {
    @NotNull String getId();    // OAuth 제공자의 고유 ID (필수)
    String getName();           // 사용자 이름 (닉네임 생성용)
}