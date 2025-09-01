package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.dto.TopicDTO;
import coffeandcommit.crema.domain.globalTag.entity.ChatTopic;
import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.globalTag.repository.ChatTopicRepository;
import coffeandcommit.crema.domain.guide.dto.request.GuideChatTopicRequestDTO;
import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideChatTopic;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.repository.GuideChatTopicRepository;
import coffeandcommit.crema.domain.guide.repository.GuideJobFieldRepository;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideMeService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;
    private final ChatTopicRepository chatTopicRepository;
    private final GuideChatTopicRepository guideChatTopicRepository;

    /* 가이드 본인 프로필 조회 */
    @Transactional(readOnly = true)
    public GuideProfileResponseDTO getGuideMeProfile(String memberId) {

        // 1. 가이드 기본 정보 조회
        Guide guide = guideRepository.findByMember_Id(memberId).orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 가이드 직무 분야 조회
        GuideJobField guideJobField = guideJobFieldRepository.findByGuide(guide).orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND));

        // 3. 연차 계산
        int workingPeriodYears = calculateWorkingPeriodYears(guide.getWorkingStart(), guide.getWorkingEnd());

        // 4. 직무분야 DTO 변환
        GuideJobFieldResponseDTO guideJobFieldResponseDTO = GuideJobFieldResponseDTO.from(guideJobField);

        return GuideProfileResponseDTO.from(guide, workingPeriodYears, guideJobFieldResponseDTO);

    }
    private int calculateWorkingPeriodYears(LocalDate workingStart, LocalDate workingEnd) {
        if (workingStart == null) {
            return 0; // 시작일이 없으면 0년으로 간주
        }
        LocalDate endDate = (workingEnd != null) ? workingEnd : LocalDate.now();
        int years = Period.between(workingStart, endDate).getYears();
        return Math.max(0, years); // 음수 방지
    }

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
            ChatTopic chatTopic = chatTopicRepository.findByChatTopicAndTopicName(topicDTO.getChatTopic(), topicDTO.getTopicName())
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
}
