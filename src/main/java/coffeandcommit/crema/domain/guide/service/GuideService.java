package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.guide.dto.response.*;
import coffeandcommit.crema.domain.guide.entity.*;
import coffeandcommit.crema.domain.guide.repository.*;
import coffeandcommit.crema.domain.review.repository.ReviewRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;
    private final GuideChatTopicRepository guideChatTopicRepository;
    private final HashTagRepository hashTagRepository;
    private final GuideScheduleRepository guideScheduleRepository;
    private final ExperienceDetailRepository experienceDetailRepository;
    private final ExperienceGroupRepository experienceGroupRepository;
    private final ReviewRepository reviewRepository;

    private void validateAccess(Guide targetGuide, String loginMemberId) {
        if (!targetGuide.isOpened()) {
            // 비공개인데 로그인 안 했거나 본인이 아니면 접근 금지
            if (loginMemberId == null || !Objects.equals(targetGuide.getMember().getId(), loginMemberId)) {
                throw new BaseException(ErrorStatus.GUIDE_NOT_FOUND);
            }
        }
    }

    /* 가이드 직무분야 조회 */
    @Transactional(readOnly = true)
    public GuideJobFieldResponseDTO getGuideJobField(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 3. 가이드 직무분야 조회
        GuideJobField guideJobField = guideJobFieldRepository.findByGuide(targetGuide)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND));

        return GuideJobFieldResponseDTO.from(guideJobField);

    }

    /* 가이드 채팅 주제 조회 */
    @Transactional(readOnly = true)
    public List<GuideChatTopicResponseDTO> getGuideChatTopics(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 3. 해당 가이드의 채팅 주제 조회
        return guideChatTopicRepository.findAllByGuideWithJoin(targetGuide).stream()
                .map(GuideChatTopicResponseDTO::from)
                .collect(Collectors.toList());
    }

    /* 가이드 해시태그 조회 */
    @Transactional(readOnly = true)
    public List<GuideHashTagResponseDTO> getGuideHashTags(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);


        // 3. 해당 가이드의 해시태그 조회
        return hashTagRepository.findByGuide(targetGuide).stream()
                .map(ht -> GuideHashTagResponseDTO.from(ht, guideId))
                .collect(Collectors.toList());
    }

    /* 가이드 스케줄 조회 */
    @Transactional(readOnly = true)
    public GuideScheduleResponseDTO getGuideSchedules(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 3. 가이드의 스케줄 전체 조회
        List<GuideSchedule> schedules = guideScheduleRepository.findByGuide(targetGuide);

        // 4. DTO 변환
        return GuideScheduleResponseDTO.from(targetGuide, schedules);
    }

    /* 가이드 경험 소주제 조회 */
    @Transactional(readOnly = true)
    public GuideExperienceDetailResponseDTO getGuideExperienceDetails(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 3. 가이드 경험 소주제 조회
        ExperienceDetail experienceDetail = experienceDetailRepository.findByGuide(targetGuide)
                .orElseThrow(() -> new BaseException(ErrorStatus.EXPERIENCE_DETAIL_NOT_FOUND));

        return GuideExperienceDetailResponseDTO.from(experienceDetail);

    }

    /* 가이드 경험 목록 조회 */
    @Transactional(readOnly = true)
    public GuideExperienceResponseDTO getGuideExperiences(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        List<ExperienceGroup> experienceGroups = experienceGroupRepository.findByGuide(targetGuide);

        return GuideExperienceResponseDTO.from(experienceGroups);
    }

    /* 가이드 커피챗 조회 */
    @Transactional(readOnly = true)
    public GuideCoffeeChatResponseDTO getGuideCoffeeChat(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 3. 태그 조회
        List<GuideHashTagResponseDTO> tags = hashTagRepository.findByGuide(targetGuide).stream()
                .map(hashTag -> GuideHashTagResponseDTO.from(hashTag, targetGuide.getId()))
                .toList();

        // 4. 리뷰 통계 조회
        Double reviewScore = Optional.ofNullable(
                        reviewRepository.getAverageScoreByGuideId(targetGuide.getId())
                ).map(score -> Math.round(score * 10.0) / 10.0)
                .orElse(0.0);

        Long reviewCount = reviewRepository.countByGuideId(targetGuide.getId());

        // 5. 경험 그룹 조회
        GuideExperienceResponseDTO experiences =
                GuideExperienceResponseDTO.from(experienceGroupRepository.findByGuide(targetGuide));

        // 6. 경험 상세 조회
        GuideExperienceDetailResponseDTO experienceDetail =
                experienceDetailRepository.findByGuide(targetGuide)
                        .map(GuideExperienceDetailResponseDTO::from)
                        .orElse(null);

        // 7. 오픈 여부 (본인일 경우 상관없이, 타인일 경우 validateAccess에서 이미 필터됨)
        boolean isOpened = targetGuide.isOpened(); // Guide 엔티티에 boolean 필드 있다고 가정

        // 8. Response DTO 변환
        return GuideCoffeeChatResponseDTO.from(
                targetGuide,
                tags,
                reviewScore,
                reviewCount,
                experiences,
                experienceDetail,
                isOpened
        );
    }
}
