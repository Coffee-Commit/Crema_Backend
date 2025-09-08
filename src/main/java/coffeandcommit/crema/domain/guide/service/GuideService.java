package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.guide.dto.response.*;
import coffeandcommit.crema.domain.guide.entity.*;
import coffeandcommit.crema.domain.guide.repository.*;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.domain.review.entity.Review;
import coffeandcommit.crema.domain.review.repository.ReviewExperienceRepository;
import coffeandcommit.crema.domain.review.repository.ReviewRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
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
    private final ReservationRepository reservationRepository;
    private final ReviewExperienceRepository reviewExperienceRepository;

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

    /* 이번 주 커피챗 예약 현황 조회 (가이드 본인만) */
    @Transactional(readOnly = true)
    public Page<GuideThisWeekCoffeeChatResponseDTO> getThisWeekCoffeeChats(Long guideId, String loginMemberId, Pageable pageable) {

        // 1. 로그인 멤버의 가이드 조회
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 본인 검증 (보안상 이중 체크)
        if (!guide.getMember().getId().equals(loginMemberId)) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 3. 이번 주 월요일 ~ 일요일 계산
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        LocalDateTime startOfWeek = monday.atStartOfDay();
        LocalDateTime endOfWeek = sunday.atTime(LocalTime.MAX);

        // 4. 예약 조회 (기간 + 상태 필터링)
        Page<Reservation> reservations = reservationRepository
                .findByGuideAndMatchingTimeBetweenAndStatusIn(
                        guide,
                        startOfWeek,
                        endOfWeek,
                        List.of(Status.CONFIRMED, Status.COMPLETED),
                        pageable
                );

        // 5. DTO 변환
        return reservations.map(reservation -> {
            LocalDateTime matchingTime = reservation.getMatchingTime();
            String dateOnly = matchingTime != null ? matchingTime.toLocalDate().toString() : null;
            String dayOfWeek = matchingTime != null
                    ? matchingTime.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                    : null;

            String timeRange = null;
            TimeUnit timeUnit = reservation.getTimeUnit();
            if (matchingTime != null && timeUnit != null && timeUnit.getTimeType() != null) {
                LocalDateTime endTime = matchingTime.plusMinutes(timeUnit.getTimeType().getMinutes());
                timeRange = matchingTime.toLocalTime().toString() + "-" + endTime.toLocalTime().toString();
            }

            return GuideThisWeekCoffeeChatResponseDTO.from(
                    reservation,
                    MemberInfo.from(reservation.getMember()),
                    dateOnly,
                    dayOfWeek,
                    timeRange
            );
        });

    }

    /* 가이드 커피챗 통계 조회 (가이드 본인만) */
    @Transactional(readOnly = true)
    public CoffeeChatStatsResponseDTO getCoffeeChatStats(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 3. 총 커피챗 완료 횟수
        Long totalCoffeeChats = reservationRepository.countByGuideAndStatus(targetGuide, Status.COMPLETED);

        // 4. 평균 별점 & 리뷰 개수
        Double averageStar = Optional.ofNullable(reviewRepository.calculateAverageStarByGuide(targetGuide))
                .map(bd -> bd.setScale(1, RoundingMode.HALF_UP).doubleValue()) // 소수점 첫째자리 반올림
                .orElse(0.0);

        Long totalReviews = reviewRepository.countByGuide(targetGuide);


        // 5. 따봉 수 (ReviewExperience 기준)
        Long thumbsUpCount = reviewExperienceRepository.countThumbsUpByGuide(targetGuide);

        // 6. DTO 변환
        return CoffeeChatStatsResponseDTO.from(totalCoffeeChats, averageStar, totalReviews, thumbsUpCount);
    }

    /* 가이드 경험별 따봉 비율 조회 */
    @Transactional(readOnly = true)
    public List<GuideExperienceEvaluationResponseDTO> getGuideExperienceEvaluations(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 2. 가이드의 경험 대주제 목록 조회
        List<ExperienceGroup> experienceGroups = experienceGroupRepository.findByGuide(targetGuide);

        // 3. 경험별 따봉 비율 계산
        return experienceGroups.stream()
                .map(group -> {
                    Long totalCount = reviewExperienceRepository.countByExperienceGroup(group);
                    Long thumbsUpCount = reviewExperienceRepository.countByExperienceGroupAndIsThumbsUpTrue(group);

                    double rate = (totalCount == 0) ? 0.0 : (double) thumbsUpCount / totalCount;

                    return GuideExperienceEvaluationResponseDTO.of(
                            group.getId(),
                            group.getExperienceTitle(),
                            rate
                    );
                })
                .toList();
    }

    /* 가이드 리뷰 목록 조회 */
    @Transactional(readOnly = true)
    public Page<GuideReviewResponseDTO> getGuideReviews(Long guideId, String loginMemberId, Pageable pageable) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 2. 리뷰 페이징 조회
        Page<Review> reviewPage = reviewRepository.findByReservation_GuideOrderByCreatedAtDesc(targetGuide, pageable);

        // 2단계 로딩: Page 안에 있는 리뷰 ID 목록 추출
        List<Long> reviewIds = reviewPage.getContent().stream()
                .map(Review::getId)
                .toList();

        // fetch join 으로 경험평가까지 로딩
        List<Review> reviewsWithExperiences = reviewRepository.findAllWithExperiencesByIdIn(reviewIds);

        // ID 기준으로 매핑 (리뷰 → 경험평가 붙인 리뷰 찾기)
        Map<Long, Review> reviewMap = reviewsWithExperiences.stream()
                .collect(Collectors.toMap(Review::getId, r -> r));

        // Page → DTO 변환 시 경험평가 포함된 리뷰 사용
        return reviewPage.map(review -> {
            Review fullReview = reviewMap.getOrDefault(review.getId(), review);
            return GuideReviewResponseDTO.from(fullReview, review.getReservation().getMember());
        });

    }

    /* 가이드 프로필 조회 */
    @Transactional(readOnly = true)
    public GuideProfileResponseDTO getGuideProfile(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        validateAccess(targetGuide, loginMemberId);

        // 2. 직무분야 → GuideJobFieldResponseDTO 변환
        GuideJobField guideJobField = targetGuide.getGuideJobField();
        if (guideJobField == null) {
            throw new BaseException(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND);
        }

        GuideJobFieldResponseDTO jobFieldDTO = GuideJobFieldResponseDTO.from(guideJobField);

        // 3. DTO 변환 후 반환
        return GuideProfileResponseDTO.from(
                targetGuide,
                targetGuide.getWorkingPeriod(), // 엔티티 필드 그대로 사용
                jobFieldDTO
        );
    }

    /* 가이드 목록 조회 */
    @Transactional(readOnly = true)
    public Page<GuideListResponseDTO> getGuides(List<Long> jobFieldIds, List<Long> chatTopicIds, String keyword, Pageable pageable, String loginMemberId, String sort) {

        boolean isPopular = "popular".equalsIgnoreCase(sort);

        // 1. popular는 전체 데이터 조회 (unpaged), latest는 DB에서 페이징
        Page<Guide> guides = isPopular
                ? guideRepository.findBySearchConditions(jobFieldIds, chatTopicIds, keyword, Pageable.unpaged())
                : guideRepository.findBySearchConditions(jobFieldIds, chatTopicIds, keyword, pageable);

        // 2. DTO 변환
        List<GuideListResponseDTO> dtoList = guides.stream()
                .map(guide -> {
                    GuideJobFieldResponseDTO jobField = GuideJobFieldResponseDTO.from(guide.getGuideJobField());

                    List<GuideHashTagResponseDTO> hashTags = guide.getHashTags().stream()
                            .map(tag -> GuideHashTagResponseDTO.from(tag, guide.getId()))
                            .toList();

                    Long totalCoffeeChats = reservationRepository.countByGuideAndStatus(guide, Status.COMPLETED);
                    Double averageStar = Optional.ofNullable(reviewRepository.calculateAverageStarByGuide(guide))
                            .map(bd -> bd.setScale(1, RoundingMode.HALF_UP).doubleValue())
                            .orElse(0.0);
                    Long totalReviews = reviewRepository.countByGuide(guide);
                    Long thumbsUpCount = reviewExperienceRepository.countThumbsUpByGuide(guide);

                    CoffeeChatStatsResponseDTO stats = CoffeeChatStatsResponseDTO.from(
                            totalCoffeeChats, averageStar, totalReviews, thumbsUpCount
                    );

                    return GuideListResponseDTO.from(
                            guide,
                            guide.getWorkingPeriod(), // 엔티티 필드 그대로 사용
                            jobField,
                            hashTags,
                            stats
                    );
                })
                .toList();

        // 3. popular일 경우 전체 정렬 후 다시 페이징
        if (isPopular) {
            dtoList = dtoList.stream()
                    .sorted(Comparator.comparing(
                            (GuideListResponseDTO dto) -> dto.getStats().getTotalReviews()
                    ).reversed())
                    .toList();

            // pageable이 unpaged일 수 있으므로 방어적으로 처리
            int page = pageable.isPaged() ? pageable.getPageNumber() : 0;
            int size = pageable.isPaged() ? pageable.getPageSize() : 20; // 기본값 20

            int start = page * size;
            int end = Math.min(start + size, dtoList.size());
            List<GuideListResponseDTO> pagedList = dtoList.subList(start, end);

            Pageable pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "modifiedAt"));

            return new PageImpl<>(pagedList, pageRequest, dtoList.size());
        }

        // 4. latest는 DB 페이징 그대로 반환
        return new PageImpl<>(dtoList, pageable, guides.getTotalElements());
    }
}
