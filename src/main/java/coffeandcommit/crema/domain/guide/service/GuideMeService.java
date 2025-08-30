package coffeandcommit.crema.domain.guide.service;

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

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuideMeService {

    private final GuideRepository guideRepository;
    private final GuideJobFieldRepository guideJobFieldRepository;

    public GuideProfileResponseDTO getGuideMeProfile(String memberId) {

        // 1. 가이드 기본 정보 조회
        Guide guide = guideRepository.findByMember_Id(memberId).orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 2. 가이드 직무 분야 조회
        GuideJobField guideJobField = guideJobFieldRepository.findByGuide(guide).orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_JOB_FIELD_NOT_FOUND));

        // 3. 연차 계산
        int workingPeriodYears = calculateWorkingPeriodYears(guide.getWorkingStart(), guide.getWorkingEnd());

        // 4. 직무분야 DTO 변환
        GuideJobFieldResponseDTO guideJobFieldResponseDTO = GuideJobFieldResponseDTO.from(guide, guideJobField);

        return GuideProfileResponseDTO.from(guide, workingPeriodYears, guideJobFieldResponseDTO);

    }
    private int calculateWorkingPeriodYears(LocalDate workingStart, LocalDate workingEnd) {
        if (workingStart == null) {
            return 0; // 시작일이 없으면 0년으로 간주
        }
        LocalDate endDate = (workingEnd != null) ? workingEnd : LocalDate.now();
        return Period.between(workingStart, endDate).getYears();
    }
}
