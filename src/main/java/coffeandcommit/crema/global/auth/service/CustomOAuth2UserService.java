package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.global.auth.provider.GoogleOAuth2UserInfo;
import coffeandcommit.crema.global.auth.provider.KakaoOAuth2UserInfo;
import coffeandcommit.crema.global.auth.provider.OAuth2UserInfo;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("이메일 정보를 찾을 수 없습니다.");
        }

        Member member = memberRepository.findByProviderAndProviderId(registrationId, userInfo.getId())
                .orElseGet(() -> createNewMember(registrationId, userInfo));

        return new CustomOAuth2User(member, oAuth2User.getAttributes());
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "kakao":
                return new KakaoOAuth2UserInfo(attributes);
            default:
                throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }
    }

    private Member createNewMember(String provider, OAuth2UserInfo userInfo) {
        Member member = Member.builder()
                .id(UUID.randomUUID().toString())
                .username(userInfo.getEmail())
                .realName(userInfo.getName())
                .nickname(userInfo.getName() + "_" + UUID.randomUUID().toString().substring(0, 8))
                .role(MemberRole.ROOKIE)
                .point(0)
                .profileImageUrl(userInfo.getImageUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .build();

        return memberRepository.save(member);
    }
}