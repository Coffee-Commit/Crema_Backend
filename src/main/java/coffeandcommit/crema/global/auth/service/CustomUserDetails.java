package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final String memberId;
    private final boolean enabled; // 사용자 활성/비활성 상태 필드 추가
    private final MemberRole memberRole; // 회원 역할 추가

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // MemberRole을 Spring Security GrantedAuthority로 변환
        String roleName = "ROLE_" + memberRole.name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return null; // JWT 방식이므로 비밀번호 불필요
    }

    @Override
    public String getUsername() {
        return memberId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled; // 실제 회원 상태 반영
    }
}