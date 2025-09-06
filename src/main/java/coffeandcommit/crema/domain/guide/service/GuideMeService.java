package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.repository.ChatTopicRepository;
import coffeandcommit.crema.domain.guide.dto.request.*;
import coffeandcommit.crema.domain.guide.dto.response.*;
import coffeandcommit.crema.domain.guide.entity.*;
import coffeandcommit.crema.domain.guide.repository.*;
import coffeandcommit.crema.domain.review.repository.ReviewRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideMeService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;
    private final ChatTopicRepository chatTopicRepository;
    private final GuideChatTopicRepository guideChatTopicRepository;
    private final HashTagRepository hashTagRepository;
    private final GuideScheduleRepository guideScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ExperienceDetailRepository experienceDetailRepository;
    private final ExperienceGroupRepository experienceGroupRepository;
    private final ReviewRepository reviewRepository;

    /* 가이드 직무 분야 등록 */
    @Transactional
    public GuideJobFieldResponseDTO registerGuideJobField(String memberId, GuideJobFieldRequestDTO guideJobFieldRequestDTO) {

        // 1. 가이드 기본 정보 조회
        Guide guide = guideRepository.findByMember_Id(memberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 요청 값 유효성 체크
        JobNameType jobName = guideJobFieldRequestDTO.getJobName();
        if (jobName == null) {
            throw new BaseException(ErrorStatus.INVALID_JOB_FIELD);
        }

        // 3. 기존 GuideJobField 조회
        GuideJobField guideJobField = guideJobFieldRepository.findByGuide(guide)
                .orElse(null);
        if (guideJobField != null) {
            // 기존 GuideJobField가 존재하면 업데이트
            guideJobField.updateJobName(jobName);
        } else {
            guideJobField = GuideJobField.builder()
                    .guide(guide)
                    .jobName(jobName)
                    .build();
        }

        // 4. GuideJobField 저장
        GuideJobField savedGuideJobField = guideJobFieldRepository.save(guideJobField);

        // 5. DTO 변환 및 반환
        return GuideJobFieldResponseDTO.from(savedGuideJobField);
    }

    /* 가이드 채팅 주제 등록 */
    @Transactional
    public List<GuideChatTopicResponseDTO> registerChatTopics(String loginMemberId, GuideChatTopicRequestDTO guideChatTopicRequestDTO) {

        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 등록 개수 제한(최대 5개)
        long currentCount = guideChatTopicRepository.countByGuide(guide);
        if (currentCount + guideChatTopicRequestDTO.getTopics().size() > 5) {
            throw new BaseException(ErrorStatus.MAX_TOPIC_EXCEEDED);
        }

        // 요청된 주제들 저장
        for (TopicDTO topicDTO : guideChatTopicRequestDTO.getTopics()) {
            // 주제가 유효한지 확인
            ChatTopic chatTopic = chatTopicRepository.findByTopicName(topicDTO.getTopicName())
                    .orElseThrow(() -> new BaseException(ErrorStatus.INVALID_TOPIC));

            // 이미 등록된 주제인지 확인
            boolean exists = guideChatTopicRepository.existsByGuideAndChatTopic(guide, chatTopic);
            if (exists) {continue;} // 중복된 주제는 건너뜀

            // GuideChatTopic 저장
            GuideChatTopic guideChatTopic = GuideChatTopic.builder()
                    .guide(guide)
                    .chatTopic(chatTopic)
                    .build();

            guideChatTopicRepository.save(guideChatTopic);
        }

        // 저장된 주제들 조회 후 DTO로 변환
        return guideChatTopicRepository.findAllByGuideWithJoin(guide).stream()
                .map(GuideChatTopicResponseDTO::from)
                .collect(Collectors.toList());

    }

    /* 가이드 채팅 주제 삭제 */
    @Transactional
    public List<GuideChatTopicResponseDTO> deleteChatTopic(String loginMemberId, Long id) {

        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 삭제할 GuideChatTopic 조회
        GuideChatTopic guideChatTopic = guideChatTopicRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_CHAT_TOPIC_NOT_FOUND));

        // 해당 GuideChatTopic이 로그인한 가이드의 것인지 확인
        if (!guideChatTopic.getGuide().getId().equals(guide.getId())) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 삭제
        guideChatTopicRepository.delete(guideChatTopic);

        // 삭제 후 남은 주제들 조회 및 DTO 변환
        return guideChatTopicRepository.findAllByGuideWithJoin(guide).stream()
                .map(GuideChatTopicResponseDTO::from)
                .collect(Collectors.toList());
    }

    /* 가이드 해시태그 등록 */
    @Transactional
    public List<GuideHashTagResponseDTO> registerGuideHashTags(
            String loginMemberId, @Valid @NotEmpty List<GuideHashTagRequestDTO> guideHashTagRequestDTOs) {

        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        long currentCount = hashTagRepository.countByGuide(guide);
        if (currentCount + guideHashTagRequestDTOs.size() > 5) {
            throw new BaseException(ErrorStatus.MAX_HASHTAG_EXCEEDED);
        }

        List<HashTag> hashTags = guideHashTagRequestDTOs.stream()
                .map(dto -> {
                    boolean exists = hashTagRepository.existsByGuideAndHashTagName(guide, dto.getHashTagName());
                    if (exists) {
                        throw new BaseException(ErrorStatus.DUPLICATE_HASHTAG);
                    }

                    return HashTag.builder()
                            .guide(guide)
                            .hashTagName(dto.getHashTagName())
                            .build();
                })
                .collect(Collectors.toList());

        List<HashTag> savedHashTags = hashTagRepository.saveAll(hashTags);

        return savedHashTags.stream()
                .map(ht -> GuideHashTagResponseDTO.from(ht, guide.getId()))
                .collect(Collectors.toList());
    }

    /* 가이드 해시태그 삭제 */
    @Transactional
    public List<GuideHashTagResponseDTO> deleteGuideHashTag(String loginMemberId, Long hashTagId) {

        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        HashTag hashTag = hashTagRepository.findById(hashTagId)
                .orElseThrow(() -> new BaseException(ErrorStatus.HASHTAG_NOT_FOUND));

        if (!hashTag.getGuide().getId().equals(guide.getId())) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        hashTagRepository.delete(hashTag);

        return hashTagRepository.findByGuide(guide).stream()
                .map(ht -> GuideHashTagResponseDTO.from(ht, guide.getId()))
                .collect(Collectors.toList());
    }

    /* 가이드 스케줄 등록 */
    @Transactional
    public GuideScheduleResponseDTO registerGuideSchedules(String loginMemberId, @Valid GuideScheduleRequestDTO guideScheduleRequestDTO) {

        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 요청 DTO -> GuideSchedule + TimeSlot 변환
        List<GuideSchedule> schedules = guideScheduleRequestDTO.getSchedules().stream()
                .map(scheduleRequestDTO -> {
                    GuideSchedule schedule = GuideSchedule.builder()
                            .guide(guide)
                            .dayOfWeek(scheduleRequestDTO.getDayOfWeek())
                            .build();

                    // TimeSlot 변환 및 연관관계 설정
                    List<TimeSlot> timeSlots = scheduleRequestDTO.getTimeSlots().stream()
                            .map(timeSlotRequestDTO -> {
                                LocalTime start = timeSlotRequestDTO.getStartTime();
                                LocalTime end = timeSlotRequestDTO.getEndTime();

                                if (start.isAfter(end) || start.equals(end)) {
                                    throw new BaseException(ErrorStatus.INVALID_TIME_RANGE);
                                }

                                validateNoOverlap(schedule.getTimeSlots(), start, end);

                                return TimeSlot.builder()
                                        .schedule(schedule)
                                        .startTimeOption(start)
                                        .endTimeOption(end)
                                        .build();
                            }).toList();

                    schedule.getTimeSlots().addAll(timeSlots);
                    return schedule;
                }).toList();

        List<GuideSchedule> savedSchedules = guideScheduleRepository.saveAll(schedules);

        return GuideScheduleResponseDTO.from(guide, savedSchedules);

    }
    /* 새로운 TimeSlot이 기존 TimeSlot 들과 겹치지 않는지 검증 */
    private void validateNoOverlap(List<TimeSlot> existingSlots, LocalTime newStart, LocalTime newEnd) {
        for (TimeSlot slot : existingSlots) {
            boolean isOverlapping = !(newEnd.isBefore(slot.getStartTimeOption()) || newStart.isAfter(slot.getEndTimeOption()));
            if (isOverlapping) {
                throw new BaseException(ErrorStatus.DUPLICATE_TIME_SLOT);
            }
        }
    }

    /* 가이드 스케줄 삭제 */
    @Transactional
    public GuideScheduleResponseDTO deleteGuideSchedule(String loginMemberId, Long timeSlotId) {

        // 1. 로그인한 사용자의 Guide 조회
        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 삭제할 TimeSlot 조회
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new BaseException(ErrorStatus.TIME_SLOT_NOT_FOUND));

        GuideSchedule schedule = timeSlot.getSchedule();

        // 3. 소유자 검증
        if (!schedule.getGuide().getId().equals(guide.getId())) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 4. 삭제 처리
        if (schedule.getTimeSlots().size() == 1) {
            // 시간대가 1개뿐이면 요일 스케줄 자체 삭제
            guideScheduleRepository.delete(schedule);
        } else {
            // 시간대만 삭제
            timeSlotRepository.delete(timeSlot);
        }

        // 5. 남은 전체 스케줄 조회 후 응답 변환
        List<GuideSchedule> remainingSchedules = guideScheduleRepository.findByGuide(guide);

        return GuideScheduleResponseDTO.from(guide, remainingSchedules);
    }

    /* 가이드 경험 소주제 등록 */
    @Transactional
    public GuideExperienceDetailResponseDTO registerExperienceDetail(String loginMemberId, @Valid GuideExperienceDetailRequestDTO guideExperienceDetailRequestDTO) {

        // 1. 로그인한 사용자의 Guide 조회
        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        String safeWho = HtmlUtils.htmlEscape(guideExperienceDetailRequestDTO.getWho(), "UTF-8");
        String safeSolution = HtmlUtils.htmlEscape(guideExperienceDetailRequestDTO.getSolution(), "UTF-8");
        String safeHow = HtmlUtils.htmlEscape(guideExperienceDetailRequestDTO.getHow(), "UTF-8");

        // 2. 기존 소주제 조회 (있으면 업데이트, 없으면 새로 생성)
        ExperienceDetail experienceDetail = experienceDetailRepository.findByGuide(guide)
                .orElseGet(() ->
                        // 없으면 새로 생성
                        ExperienceDetail.builder()
                                .guide(guide)
                                .build()
                );

        // 3. 필드 덮어쓰기
        experienceDetail = experienceDetail.toBuilder()
                .who(safeWho)
                .solution(safeSolution)
                .how(safeHow)
                .build();

        // 4. 저장
        ExperienceDetail savedDetail = experienceDetailRepository.save(experienceDetail);

        // 5. DTO 변환 및 반환
        return GuideExperienceDetailResponseDTO.from(savedDetail);

    }

    /* 가이드 경험 소주제 삭제 */
    @Transactional
    public GuideExperienceDetailResponseDTO deleteExperienceDetail(String loginMemberId, Long experienceDetailId) {

        // 1. 로그인한 사용자의 Guide 조회
        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 삭제할 ExperienceDetail 조회
        ExperienceDetail experienceDetail = experienceDetailRepository.findById(experienceDetailId)
                .orElseThrow(() -> new BaseException(ErrorStatus.EXPERIENCE_DETAIL_NOT_FOUND));

        // 3. 소유자 검증
        if (!experienceDetail.getGuide().getId().equals(guide.getId())) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 4. 삭제 처리
        experienceDetailRepository.delete(experienceDetail);

        // 5. 삭제된 경험 소주제 정보 반환 (필요시)
        return GuideExperienceDetailResponseDTO.from(experienceDetail);
    }

    /* 가이드 경험 목록 등록 */
    @Transactional
    public GuideExperienceResponseDTO registerGuideExperience(String loginMemberId, @Valid GuideExperienceRequestDTO guideExperienceRequestDTO) {

        // 1. 로그인한 사용자의 Guide 조회
        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 등록 개수 제한 체크
        long currentCount = experienceGroupRepository.countByGuide(guide);
        if (currentCount + guideExperienceRequestDTO.getGroups().size() > 6) {
            throw new BaseException(ErrorStatus.EXPERIENCE_LIMIT_EXCEEDED);
        }


        // 3. ExperienceGroup 생성
        List<ExperienceGroup> experienceGroups = guideExperienceRequestDTO.getGroups().stream()
                .map(groupReq  -> {
                    GuideChatTopic guideChatTopic = guideChatTopicRepository.findById(groupReq.getGuideChatTopicId())
                            .orElseThrow(() -> new BaseException(ErrorStatus.INVALID_GUIDE_CHAT_TOPIC));

                    if (!guideChatTopic.getGuide().getId().equals(guide.getId())) {
                        throw new BaseException(ErrorStatus.FORBIDDEN);
                    }

                    return ExperienceGroup.builder()
                            .guide(guide)
                            .guideChatTopic(guideChatTopic)
                            .experienceTitle(groupReq.getExperienceTitle())
                            .experienceContent(groupReq.getExperienceContent())
                            .build();
                }).toList();

        // 4. 저장
        List<ExperienceGroup> savedGroups = experienceGroupRepository.saveAll(experienceGroups);

        // 5. Response 변환
        return GuideExperienceResponseDTO.from(savedGroups);
    }

    /* 가이드 경험 목록 삭제 */
    @Transactional
    public GuideExperienceResponseDTO deleteGuideExperience(String loginMemberId, Long experienceId) {

        // 1. 로그인한 사용자의 Guide 조회
        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 삭제할 ExperienceGroup 조회
        ExperienceGroup experienceGroup = experienceGroupRepository.findById(experienceId)
                .orElseThrow(() -> new BaseException(ErrorStatus.EXPERIENCE_NOT_FOUND));

        // 3. 소유자 검증
        if (!experienceGroup.getGuide().getId().equals(guide.getId())) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 4. 삭제 처리
        experienceGroupRepository.delete(experienceGroup);

        // 5. 남은 경험 목록 조회 및 응답 변환
        List<ExperienceGroup> remainingGroups = experienceGroupRepository.findByGuide(guide);

        return GuideExperienceResponseDTO.from(remainingGroups);
    }

    /* 가이드 커피챗 소개글 등록 */
    @Transactional
    public GuideCoffeeChatResponseDTO registerGuideCoffeeChat(String loginMemberId, @Valid GuideCoffeeChatRequestDTO requestDTO) {

        // 1. 로그인 멤버의 가이드 조회
        Guide guide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 본인 검증 (보안상 이중 체크)
        if (!guide.getMember().getId().equals(loginMemberId)) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 3. Guide 엔티티 업데이트 (제목/소개글) → updatedGuide로 새 변수 사용
        Guide updatedGuide = guide.toBuilder()
                .title(requestDTO.getTitle())
                .chatDescription(requestDTO.getChatDescription())
                .isOpened(true)
                .build();

        guideRepository.save(updatedGuide);

        // 4. 태그 조회 (GuideHashTagResponseDTO 리스트 변환)
        List<GuideHashTagResponseDTO> tags = hashTagRepository.findByGuide(updatedGuide).stream()
                .map(hashTag -> GuideHashTagResponseDTO.from(hashTag, updatedGuide.getId()))
                .toList();

        // 5. 리뷰 통계 조회 (평균 별점 / 리뷰 개수)
        Double reviewScore = Optional.ofNullable(
                        reviewRepository.getAverageScoreByGuideId(updatedGuide.getId())
                ).map(score -> Math.round(score * 10.0) / 10.0) // 소수점 첫째 자리 반올림
                .orElse(0.0);
        Long reviewCount = reviewRepository.countByGuideId(updatedGuide.getId());

        // 6. 경험 그룹 조회
        GuideExperienceResponseDTO experiences =
                GuideExperienceResponseDTO.from(experienceGroupRepository.findByGuide(updatedGuide));

        // 7. 경험 상세 조회 (단건)
        GuideExperienceDetailResponseDTO experienceDetail =
                experienceDetailRepository.findByGuide(updatedGuide)
                        .map(GuideExperienceDetailResponseDTO::from)
                        .orElse(null);

        // 8. Response DTO 변환
        return GuideCoffeeChatResponseDTO.from(
                updatedGuide,
                tags,
                reviewScore,
                reviewCount,
                experiences,
                experienceDetail,
                updatedGuide.isOpened()
        );
    }
}
