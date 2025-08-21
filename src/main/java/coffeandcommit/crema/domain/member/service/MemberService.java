package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * ID로 회원 조회
     */
    public MemberResponse getMemberById(String id) {
        Member member = findMemberById(id);
        return MemberMapper.INSTANCE.memberToMemberResponse(member);
    }

    /**
     * 사용자 아이디로 회원 조회 (로그인 아이디 = 이메일)
     */
    public MemberResponse getMemberByUserId(String userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        return MemberMapper.INSTANCE.memberToMemberResponse(member);
    }

    /**
     * 닉네임으로 회원 조회
     */
    public MemberResponse getMemberByNickname(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
        return MemberMapper.INSTANCE.memberToMemberResponse(member);
    }

    /**
     * 회원 프로필 업데이트
     */
    @Transactional
    public MemberResponse updateMemberProfile(String id, String nickname, String description, String profileImageUrl) {
        Member member = findMemberById(id);

        // 닉네임 중복 체크 (본인 제외)
        if (nickname != null && !nickname.trim().isEmpty() &&
                !nickname.equals(member.getNickname()) &&
                memberRepository.existsByNickname(nickname)) {
            throw new BaseException(ErrorStatus.NICKNAME_DUPLICATED);
        }

        // 닉네임 유효성 검사
        if (nickname != null && !isValidNickname(nickname)) {
            throw new BaseException(ErrorStatus.INVALID_NICKNAME_FORMAT);
        }

        member.updateProfile(nickname, description, profileImageUrl);
        Member savedMember = memberRepository.save(member);

        log.info("Member profile updated: {}", id);
        return MemberMapper.INSTANCE.memberToMemberResponse(savedMember);
    }

    /**
     * 회원 삭제
     */
    @Transactional
    public void deleteMember(String id) {
        if (!memberRepository.existsById(id)) {
            throw new BaseException(ErrorStatus.MEMBER_NOT_FOUND);
        }
        memberRepository.deleteById(id);
        log.info("Member deleted: {}", id);
    }

    /**
     * 사용자 아이디 사용 가능 여부 확인
     */
    public boolean isUserIdAvailable(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        return !memberRepository.existsByUserId(userId);
    }

    /**
     * 닉네임 사용 가능 여부 확인
     */
    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        return !memberRepository.existsByNickname(nickname) && isValidNickname(nickname);
    }

    /**
     * 회원 포인트 조회
     */
    public int getMemberPoints(String id) {
        Member member = findMemberById(id);
        return member.getPoint();
    }

    /**
     * 회원 포인트 추가
     */
    @Transactional
    public void addPoints(String id, int point) {
        Member member = findMemberById(id);
        member.addPoint(point);
        memberRepository.save(member);
        log.info("Points added to member {}: +{}", id, point);
    }

    /**
     * 회원 포인트 차감
     */
    @Transactional
    public void decreasePoints(String id, int point) {
        Member member = findMemberById(id);
        member.decreasePoint(point);
        memberRepository.save(member);
        log.info("Points decreased from member {}: -{}", id, point);
    }

    // === Private Helper Methods ===

    private Member findMemberById(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));
    }

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
}