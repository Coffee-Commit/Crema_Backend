package coffeandcommit.crema.domain.member.service;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.member.dto.request.MemberUpgradeRequest;
import coffeandcommit.crema.domain.member.dto.response.MemberUpgradeResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberPublicResponse;
import coffeandcommit.crema.domain.member.dto.response.MemberResponse;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.enums.MemberRole;
import coffeandcommit.crema.domain.member.mapper.MemberMapper;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final GuideRepository guideRepository;
    private final MemberMapper memberMapper;
    private final MemberProfileService memberProfileService;

    /**
     * 8글자 UUID 생성 (중복 체크 포함)
     */
    public String generateId() {
        String id;
        int attempts = 0;
        final int maxAttempts = 10;

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
     * Member 저장 시 ID 충돌을 자동으로 재시도하는 안전한 저장 메서드
     */
    @Transactional
    public Member saveWithRetry(Supplier<Member> memberSupplier) {
        int attempts = 0;
        final int maxAttempts = 10;

        while (attempts < maxAttempts) {
            attempts++;

            try {
                Member member = memberSupplier.get();
                Member savedMember = memberRepository.saveAndFlush(member);

                log.debug("Member saved successfully: {} (attempts: {})", member.getId(), attempts);
                return savedMember;

            } catch (DataIntegrityViolationException e) {
                log.debug("ID collision detected during save (attempt {}/{})", attempts, maxAttempts);

                if (attempts >= maxAttempts) {
                    log.error("Failed to save member after {} attempts due to ID collisions", maxAttempts);
                    throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * ID로 회원 조회 - 본인용 (모든 정보 포함)
     */
    public MemberResponse getMyInfo(String id) {
        Member member = findActiveMemberById(id);
        return convertToMemberResponse(member);
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
        return convertToMemberResponse(savedMember);
    }

    /**
     * 가이드로 업그레이드
     */
    @Transactional
    public MemberUpgradeResponse upgradeToGuide(String memberId, MemberUpgradeRequest request) {
        Member member = findActiveMemberById(memberId);

        // 이미 가이드인지 확인
        if (member.getRole() == MemberRole.GUIDE) {
            throw new BaseException(ErrorStatus.ALREADY_GUIDE);
        }

        // 이미 Guide 엔티티가 존재하는지 확인
        if (guideRepository.findByMember_Id(memberId).isPresent()) {
            throw new BaseException(ErrorStatus.ALREADY_GUIDE);
        }

        // 유효성 검사
        validateWorkingPeriod(request);

        // Member 역할 변경
        member = member.toBuilder()
                .role(MemberRole.GUIDE)
                .build();
        member = memberRepository.save(member);

        // Guide 엔티티 생성
        Guide guide = createGuideEntity(member, request);
        guide = guideRepository.save(guide);

        // 응답 생성
        return createUpgradeResponse(guide, request);
    }

    /**
     * 가이드 업그레이드 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberUpgradeResponse getUpgradeInfo(String memberId) {
        Member member = findActiveMemberById(memberId);

        // 가이드인지 확인
        if (member.getRole() != MemberRole.GUIDE) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        Guide guide = guideRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        return createUpgradeResponseFromGuide(guide);
    }

    /**
     * 가이드 업그레이드 정보 수정
     */
    @Transactional
    public MemberUpgradeResponse updateUpgradeInfo(String memberId, MemberUpgradeRequest request) {
        Member member = findActiveMemberById(memberId);

        // 가이드인지 확인
        if (member.getRole() != MemberRole.GUIDE) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        Guide guide = guideRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 유효성 검사
        validateWorkingPeriod(request);

        // Guide 정보 업데이트
        guide = guide.toBuilder()
                .companyName(request.getCompanyName())
                .jobPosition(request.getJobPosition())
                .workingStart(request.getWorkingStart())
                .workingEnd(request.getWorkingEnd())
                .isCurrent(request.getIsCurrent())
                // TODO: 회사명 공개 여부 필드 추가 후 설정
                // .isCompanyNamePublic(request.getIsCompanyNamePublic())
                .build();

        guide = guideRepository.save(guide);

        return createUpgradeResponse(guide, request);
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
            }
        }

        member.softDelete();
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
     * Member를 MemberResponse로 변환 (기본 멤버 정보만)
     */
    private MemberResponse convertToMemberResponse(Member member) {
        return memberMapper.memberToMemberResponse(member);
    }

    /**
     * 근무 기간 유효성 검사
     */
    private void validateWorkingPeriod(MemberUpgradeRequest request) {
        // 재직중인 경우 근무 끝 날짜는 null이어야 함
        if (request.getIsCurrent() && request.getWorkingEnd() != null) {
            throw new BaseException(ErrorStatus.WORKING_END_NOT_ALLOWED_WHEN_CURRENT);
        }

        // 재직중이 아닌 경우 근무 끝 날짜가 필요
        if (!request.getIsCurrent() && request.getWorkingEnd() == null) {
            throw new BaseException(ErrorStatus.WORKING_END_REQUIRED_WHEN_NOT_CURRENT);
        }

        // 근무 시작일이 끝날보다 늦을 수 없음
        if (request.getWorkingEnd() != null && request.getWorkingStart().isAfter(request.getWorkingEnd())) {
            throw new BaseException(ErrorStatus.INVALID_WORKING_PERIOD);
        }
    }

    /**
     * Guide 엔티티 생성
     */
    private Guide createGuideEntity(Member member, MemberUpgradeRequest request) {
        return Guide.builder()
                .member(member)
                .isOpened(false) // 처음엔 비공개
                .title("") // 빈 제목으로 시작
                .chatDescription("") // 빈 설명으로 시작
                .companyName(request.getCompanyName())
                .jobPosition(request.getJobPosition())
                .workingStart(request.getWorkingStart())
                .workingEnd(request.getWorkingEnd())
                .isCurrent(request.getIsCurrent())
                .isCompanyNamePublic(request.getIsCompanyNamePublic())
                // TODO: PDF 업로드 필드 연동 예정
                // .certificationImageUrl(request.getCertificationPdfUrl())
                .build();
    }

    /**
     * MemberUpgradeResponse 생성 (요청값 포함)
     */
    private MemberUpgradeResponse createUpgradeResponse(Guide guide, MemberUpgradeRequest request) {
        int workingPeriodYears = calculateWorkingPeriodYears(guide.getWorkingStart(), guide.getWorkingEnd());

        return MemberUpgradeResponse.builder()
                .guideId(guide.getId())
                .companyName(guide.getCompanyName())
                .isCompanyNamePublic(request.getIsCompanyNamePublic()) // 요청값 그대로 반환
                .jobPosition(guide.getJobPosition())
                .isCurrent(guide.isCurrent())
                .workingStart(guide.getWorkingStart())
                .workingEnd(guide.getWorkingEnd())
                .workingPeriodYears(workingPeriodYears)
                .isOpened(guide.isOpened())
                .title(guide.getTitle())
                .chatDescription(guide.getChatDescription())
                .build();
    }

    /**
     * MemberUpgradeResponse 생성 (Guide 엔티티만으로)
     */
    private MemberUpgradeResponse createUpgradeResponseFromGuide(Guide guide) {
        int workingPeriodYears = calculateWorkingPeriodYears(guide.getWorkingStart(), guide.getWorkingEnd());

        return MemberUpgradeResponse.builder()
                .guideId(guide.getId())
                .companyName(guide.getCompanyName())
                .isCompanyNamePublic(guide.isCompanyNamePublic())
                .jobPosition(guide.getJobPosition())
                .isCurrent(guide.isCurrent())
                .workingStart(guide.getWorkingStart())
                .workingEnd(guide.getWorkingEnd())
                .workingPeriodYears(workingPeriodYears)
                .isOpened(guide.isOpened())
                .title(guide.getTitle())
                .chatDescription(guide.getChatDescription())
                .build();
    }

    /**
     * 근무 기간 연차 계산
     */
    private int calculateWorkingPeriodYears(LocalDate workingStart, LocalDate workingEnd) {
        if (workingStart == null) {
            return 0;
        }
        LocalDate endDate = (workingEnd != null) ? workingEnd : LocalDate.now();
        int years = Period.between(workingStart, endDate).getYears();
        return Math.max(0, years);
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