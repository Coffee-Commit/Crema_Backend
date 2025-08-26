package coffeandcommit.crema.global.auth.provider;

import jakarta.validation.constraints.NotNull;

public interface OAuth2UserInfo {
    @NotNull String getId();
    String getName();
    String getEmail();
    String getImageUrl();
}