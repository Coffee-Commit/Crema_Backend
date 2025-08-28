package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.member.dto.response.MemberPublicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final MemberProfileService memberProfileService;

    /**
     * 8글자 UUID 생성 (중복 체크 포함)
     */
    public String generateId() {
        String id;
        int attempts = 0;
        final int maxAttempts = 10; // 무한 루프 방지

        do {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toLowerCase();
            attempts++;

            if (attempts >= maxAttempts) {
                log.error("Failed to generate unique member ID after {} attempts", maxAttempts);
                throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
            }
        } while (memberRepository.existsById(id));

        log.debug("Generated unique member ID: {} (attempts: {})", id, attempts);
        return id;
    }

    /**
     * ID로 회원 조회 - 본인용 (모든 정보 포함)
     */
    public MemberResponse getMyInfo(String id) {
        Member member = findActiveMemberById(id);
        return memberMapper.memberToMemberResponse(member);
    }

    /**
     * ID로 회원 조회 - 타인용 (공개 정보만)
     */
    public MemberPublicResponse getMemberById(String id) {
        Member member = findActiveMemberById(id);
        return memberMapper.memberToMemberPublicResponse(member);
    }

    /**
     * 닉네임으로 회원 조회 - 타인용 (공개 정보만)
     */
    public MemberPublicResponse getMemberByNickname(String nickname) {
        Member member = memberRepository.findByNicknameAndIsDeletedFalse(nickname)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        return memberMapper.memberToMemberPublicResponse(member);
    }

    /**
     * 회원 프로필 정보 업데이트 (닉네임, 자기소개, 이메일)
     */
    @Transactional
    public MemberResponse updateMemberProfileInfo(String id, String nickname, String description, String email) {
        Member member = findActiveMemberById(id);

        // 닉네임 중복 체크 (본인 제외, 활성 회원만)
        if (nickname != null && !nickname.trim().isEmpty() &&
                !nickname.equals(member.getNickname()) &&
                memberRepository.existsByNicknameAndIsDeletedFalse(nickname)) {
            throw new BaseException(ErrorStatus.NICKNAME_DUPLICATED);
        }

        // 닉네임 유효성 검사
        if (nickname != null && !isValidNickname(nickname)) {
            throw new BaseException(ErrorStatus.INVALID_NICKNAME_FORMAT);
        }

        // 이메일 유효성 검사
        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            throw new BaseException(ErrorStatus.INVALID_EMAIL_FORMAT);
        }

        member.updateProfile(nickname, description, member.getProfileImageUrl(), email);
        Member savedMember = memberRepository.save(member);

        log.info("Member profile info updated: {}", id);
        return memberMapper.memberToMemberResponse(savedMember);
    }

    /**
     * 회원 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteMember(String id) {
        Member member = findActiveMemberById(id);

        // 프로필 이미지가 있다면 삭제
        if (member.getProfileImageUrl() != null) {
            try {
                memberProfileService.deleteProfileImage(id);
            } catch (Exception e) {
                log.error("Failed to delete profile image for member: {} - Error: {}", id, e.getMessage(), e);
                // 프로필 이미지 삭제 실패해도 회원 탈퇴는 계속 진행
            }
        }

        member.softDelete(); // isDeleted = true로 변경
        memberRepository.save(member);
        log.info("Member soft deleted: {}", id);
    }

    /**
     * 닉네임 사용 가능 여부 확인 (활성 회원만 체크)
     */
    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        return !memberRepository.existsByNicknameAndIsDeletedFalse(nickname) && isValidNickname(nickname);
    }

    /**
     * 회원 포인트 조회
     */
    public int getMemberPoints(String id) {
        Member member = findActiveMemberById(id);
        return member.getPoint();
    }

    /**
     * 회원 포인트 추가
     */
    @Transactional
    public void addPoints(String id, int point) {
        Member member = findActiveMemberById(id);
        member.addPoint(point);
        memberRepository.save(member);
        log.info("Points added to member {}: +{}", id, point);
    }

    /**
     * 회원 포인트 차감
     */
    @Transactional
    public void decreasePoints(String id, int point) {
        Member member = findActiveMemberById(id);
        member.decreasePoint(point);
        memberRepository.save(member);
        log.info("Points decreased from member {}: -{}", id, point);
    }

    /**
     * JWT 인증을 위한 UserDetails 생성
     */
    public CustomUserDetails createUserDetails(String memberId) {
        Member member = findActiveMemberById(memberId);
        // 탈퇴하지 않은 회원만 enabled=true
        boolean enabled = !Boolean.TRUE.equals(member.getIsDeleted());
        return new CustomUserDetails(memberId, enabled, member.getRole());
    }

    // === Private Helper Methods ===

    /**
     * 활성 회원만 조회하는 안전한 메서드
     */
    private Member findActiveMemberById(String id) {
        return memberRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    /**
     * 닉네임 유효성 검사
     */
    private boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }

        String trimmed = nickname.trim();

        // 길이 체크 (2-32자)
        if (trimmed.length() < 2 || trimmed.length() > 32) {
            return false;
        }

        // 한글, 영문, 숫자, 언더스코어만 허용
        return trimmed.matches("^[가-힣a-zA-Z0-9_]+$");
    }

    /**
     * 이메일 유효성 검사
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmed = email.trim();

        // 길이 체크 (최대 320자)
        if (trimmed.length() > 320) {
            return false;
        }

        // @ 정확히 1개만 허용
        if (trimmed.indexOf('@') != trimmed.lastIndexOf('@')) {
            return false;
        }

        // 기본적인 잘못된 패턴 체크
        if (trimmed.startsWith(".") || trimmed.endsWith(".") ||
                trimmed.startsWith("@") || trimmed.endsWith("@") ||
                trimmed.contains("..") || trimmed.contains("@.") || trimmed.contains(".@")) {
            return false;
        }

        // 영문, 숫자, 일부 특수문자(.-_)만 허용, 도메인은 2-8자 제한
        return trimmed.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,8}$");
    }
}
