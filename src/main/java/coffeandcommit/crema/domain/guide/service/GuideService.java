package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideHashTagResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideScheduleResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.entity.GuideSchedule;
import coffeandcommit.crema.domain.guide.repository.*;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
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
}
