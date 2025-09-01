package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.guide.dto.response.GuideChatTopicResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;
    private final GuideChatTopicRepository guideChatTopicRepository;

    /* 가이드 직무분야 조회 */
    @Transactional(readOnly = true)
    public GuideJobFieldResponseDTO getGuideJobField(Long guideId, String loginMemberId) {

        // 1. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));


        // 2. 공개 여부 체크
        if(!targetGuide.isOpened()){
            // 비공개 가이드인 경우, 본인 가이드가 아니면 접근 불가
            if (loginMemberId == null) {
                throw new BaseException(ErrorStatus.FORBIDDEN);
            }

            // 1. 로그인한 사용자가 가이드인지 확인
            Guide myGuide = guideRepository.findByMember_Id(loginMemberId)
                    .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

            // 본인 가이드가 아니면 접근 불가
            if (!myGuide.getId().equals(targetGuide.getId())) {
                throw new BaseException(ErrorStatus.FORBIDDEN);
            }
        }

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


        // 2. 공개 여부 체크
        if(!targetGuide.isOpened()){
            // 비공개 가이드일 경우 로그인 필요
            if (loginMemberId == null) {
                throw new BaseException(ErrorStatus.FORBIDDEN);
            }
            // 로그인한 사용자가 가이드인지 확인
            Guide myGuide = guideRepository.findByMember_Id(loginMemberId)
                    .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

            // 본인 가이드가 아니면 접근 불가
            if (!myGuide.getId().equals(targetGuide.getId())) {
                throw new BaseException(ErrorStatus.FORBIDDEN);
            }
        }

        // 3. 해당 가이드의 채팅 주제 조회
        return guideChatTopicRepository.findAllByGuideWithJoin(targetGuide).stream()
                .map(GuideChatTopicResponseDTO::from)
                .collect(Collectors.toList());
    }
}
