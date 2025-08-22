package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.global.auth.provider.GoogleOAuth2UserInfo;
import coffeandcommit.crema.global.auth.provider.KakaoOAuth2UserInfo;
import coffeandcommit.crema.global.auth.provider.OAuth2UserInfo;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception e) {
            log.error("OAuth2 user loading failed: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("OAuth2 사용자 정보 로딩에 실패했습니다.");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 이메일 처리 - 없으면 임시 이메일 생성
        final String email = StringUtils.hasText(userInfo.getEmail())
                ? userInfo.getEmail()
                : String.format("%s_%s@temp.crema.com", registrationId, userInfo.getId());

        // 기존 사용자 확인 또는 새 사용자 생성
        Member member = memberRepository.findByProviderAndProviderId(registrationId, userInfo.getId())
                .orElseGet(() -> {
                    // 이메일로 기존 사용자 확인 (다른 OAuth 제공자로 이미 가입한 경우)
                    return memberRepository.findByUserId(email)
                            .map(existingMember -> linkOAuthAccount(existingMember, registrationId, userInfo))
                            .orElseGet(() -> createNewMember(registrationId, userInfo, email));
                });

        // 탈퇴한 사용자의 경우 예외 처리
        if (member.getIsDeleted()) {
            throw new OAuth2AuthenticationException("탈퇴한 계정입니다.");
        }

        // 사용자 정보 업데이트 (프로필 이미지, 이름 등이 변경될 수 있음)
        updateMemberInfo(member, userInfo);

        return new CustomOAuth2User(member, oAuth2User.getAttributes());
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "kakao":
                return new KakaoOAuth2UserInfo(attributes);
            default:
                log.error("Unsupported OAuth2 provider: {}", registrationId);
                throw new BaseException(ErrorStatus.UNSUPPORTED_OAUTH2_PROVIDER);
        }
    }

    private Member createNewMember(String provider, OAuth2UserInfo userInfo, String email) {
        String uniqueNickname = generateUniqueNickname(userInfo.getName());

        Member member = Member.builder()
                .id(UUID.randomUUID().toString())
                .userId(email) // 실제 이메일 또는 임시 이메일
                .nickname(uniqueNickname)
                .role(MemberRole.ROOKIE)
                .point(0) // 초기 포인트
                .profileImageUrl(userInfo.getImageUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .build();

        log.info("Creating new member with provider: {}, email: {}, nickname: {}",
                provider, email, uniqueNickname);

        return memberRepository.save(member);
    }

    private Member linkOAuthAccount(Member existingMember, String provider, OAuth2UserInfo userInfo) {
        // 기존 사용자에게 OAuth 정보 연결
        Member updatedMember = existingMember.toBuilder()
                .provider(provider)
                .providerId(userInfo.getId())
                .build();

        log.info("Linking OAuth account to existing member: {} with provider: {}",
                existingMember.getId(), provider);

        return memberRepository.save(updatedMember);
    }

    private void updateMemberInfo(Member member, OAuth2UserInfo userInfo) {
        boolean updated = false;

        // 프로필 이미지 업데이트 (기존에 없거나 OAuth 제공자의 이미지가 더 최신인 경우)
        if (StringUtils.hasText(userInfo.getImageUrl()) &&
                !StringUtils.hasText(member.getProfileImageUrl())) {
            member = member.toBuilder()
                    .profileImageUrl(userInfo.getImageUrl())
                    .build();
            updated = true;
        }

        if (updated) {
            memberRepository.save(member);
            log.info("Updated member info for: {}", member.getId());
        }
    }

    private String generateUniqueNickname(String baseName) {
        if (!StringUtils.hasText(baseName)) {
            baseName = "사용자";
        }

        // 기본 이름 정리
        String cleanedBaseName = cleanName(baseName);
        if (cleanedBaseName.length() > 10) {
            cleanedBaseName = cleanedBaseName.substring(0, 10);
        }

        String nickname = cleanedBaseName;

        // 중복 체크 후 UUID 추가
        if (memberRepository.existsByNickname(nickname)) {
            String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
            nickname = cleanedBaseName + "_" + randomSuffix;

            // 극히 드문 경우의 최종 안전장치 (UUID 충돌 시)
            if (memberRepository.existsByNickname(nickname)) {
                nickname = cleanedBaseName + "_" + System.currentTimeMillis() % 100000;
            }
        }

        return nickname;
    }

    private String cleanName(String name) {
        if (!StringUtils.hasText(name)) {
            return "사용자";
        }

        // 이름 정리: 특수문자 제거, 공백 정리
        String cleaned = name.trim()
                .replaceAll("[^가-힣a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            return "사용자";
        }

        // 최대 길이 제한
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }

        return cleaned;
    }
}