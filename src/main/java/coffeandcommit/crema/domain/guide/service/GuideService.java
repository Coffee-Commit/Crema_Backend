package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.GuideJobField;
import coffeandcommit.crema.domain.guide.repository.GuideJobFieldRepository;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;

    @Transactional(readOnly = true)
    public GuideJobFieldResponseDTO getGuideJobField(Long guideId, String loginMemberId) {

        // 1. 로그인한 사용자가 가이드인지 확인
        Guide myGuide = guideRepository.findByMember_Id(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 조회 대상 가이드 조회
        Guide targetGuide = guideRepository.findById(guideId)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 3. 공개 여부 체크
        if(!targetGuide.isOpened()){
            // 비공개 가이드인 경우, 본인 가이드가 아니면 접근 불가
            if(!myGuide.getId().equals(targetGuide.getId())){
                throw new BaseException(ErrorStatus.FORBIDDEN);
            }
        }

        // 4. 가이드 직무분야 조회
        GuideJobField guideJobField = guideJobFieldRepository.findByGuide(targetGuide)
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND));

        return GuideJobFieldResponseDTO.from(guideJobField);

    }
}
