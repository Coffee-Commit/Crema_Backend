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

        // 기존 사용자 확인 또는 새 사용자 생성 (활성 회원만 조회)
        Member member = memberRepository.findByProviderAndProviderIdAndIsDeletedFalse(registrationId, userInfo.getId())
                .orElseGet(() -> createNewMember(registrationId, userInfo));

        // 탈퇴한 사용자의 경우 예외 처리 (추가 안전장치)
        if (Boolean.TRUE.equals(member.getIsDeleted())) {
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

    private Member createNewMember(String provider, OAuth2UserInfo userInfo) {
        String uniqueNickname = generateUniqueNickname(userInfo.getName());

        Member member = Member.builder()
                .id(UUID.randomUUID().toString())
                .nickname(uniqueNickname)
                .role(MemberRole.ROOKIE)
                .point(0) // 초기 포인트
                .profileImageUrl(userInfo.getImageUrl())
                .provider(provider)
                .providerId(userInfo.getId())
                .build();

        log.info("Creating new member with provider: {}, nickname: {}",
                provider, uniqueNickname);

        return memberRepository.save(member);
    }

    private void updateMemberInfo(Member member, OAuth2UserInfo userInfo) {
        boolean updated = false;

        // 프로필 이미지 업데이트 (기존에 없거나 OAuth 제공자의 이미지가 더 최신인 경우)
        if (StringUtils.hasText(userInfo.getImageUrl()) &&
                !StringUtils.hasText(member.getProfileImageUrl())) {

            member.updateProfile(null, null, userInfo.getImageUrl());
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

        String cleanedBaseName = cleanName(baseName);
        if (cleanedBaseName.length() > 20) {
            cleanedBaseName = cleanedBaseName.substring(0, 20);
        }

        // 항상 UUID 추가
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        String nickname = cleanedBaseName + "_" + randomSuffix;

        // uuid가 1/1500만의 확률로 중복되면 타임 스탬프로 닉네임 생성 (활성 회원만 체크)
        while (memberRepository.existsByNicknameAndIsDeletedFalse(nickname)) {
            randomSuffix = UUID.randomUUID().toString().substring(0, 6);
            nickname = cleanedBaseName + "_" + randomSuffix;
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