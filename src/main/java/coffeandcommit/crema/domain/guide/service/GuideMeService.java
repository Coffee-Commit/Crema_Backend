package coffeandcommit.crema.domain.guide.service;

import coffeandcommit.crema.domain.globalTag.enums.JobNameType;
import coffeandcommit.crema.domain.guide.dto.request.GuideJobFieldRequestDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideJobFieldResponseDTO;
import coffeandcommit.crema.domain.guide.dto.response.GuideProfileResponseDTO;
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

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideMeService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;

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


}
