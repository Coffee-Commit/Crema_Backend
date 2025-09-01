package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.domain.member.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // MemberRole을 Spring Security GrantedAuthority로 변환
        String roleName = "ROLE_" + member.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getName() {
        return member.getId(); // member ID 반환
    }
}