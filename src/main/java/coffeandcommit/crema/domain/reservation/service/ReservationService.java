package coffeandcommit.crema.domain.reservation.service;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.guide.enums.TimeType;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.dto.request.ReservationRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationResponseDTO;
import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.entity.Survey;
import coffeandcommit.crema.domain.reservation.entity.SurveyFile;
import coffeandcommit.crema.domain.reservation.enums.Status;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final GuideRepository guideRepository;

    /* 예약 존재 여부 확인 */
    @Transactional(readOnly = true)
    public Reservation getReservationOrThrow(Long reservationId) {
        if (reservationId == null) {
            throw new BaseException(ErrorStatus.INVALID_RESERVATION_ID);
        }
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BaseException(ErrorStatus.RESERVATION_NOT_FOUND));
    }

    /* 커피챗 예약 신청 */
    @Transactional
    public ReservationResponseDTO createReservation(String loginMemberId, @Valid ReservationRequestDTO reservationRequestDTO) {

        // 1. 로그인한 사용자의 Guide 조회
        Member member = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new BaseException(ErrorStatus.MEMBER_NOT_FOUND));

        // 2. 존재하는 가이드인지 확인
        Guide guide = guideRepository.findById(reservationRequestDTO.getGuideId())
                .orElseThrow(() -> new BaseException(ErrorStatus.GUIDE_NOT_FOUND));

        // 3. Survey 엔티티 생성
        Survey survey = Survey.builder()
                .messageToGuide(reservationRequestDTO.getSurvey().getMessageToGuide())
                .preferredDate(reservationRequestDTO.getSurvey().getPreferredDate())
                .build();

        // SurveyFile 엔티티 리스트 생성
        List<SurveyFile> files = reservationRequestDTO.getSurvey().getFiles().stream()
                .map(fileDto -> SurveyFile.builder()
                        .survey(survey)
                        .fileUploadUrl(fileDto.getFileUploadUrl())
                        .build())
                .toList();

        survey.getFiles().addAll(files);


        // 4. Reservation 엔티티 생성
        Reservation reservation = Reservation.builder()
                .member(member)
                .guide(guide)
                .status(Status.PENDING)
                .survey(survey)
                .build();

        // 5. TimeUnit 엔티티 생성 (예약 ↔ 시간 단위 연결)
        TimeUnit timeUnit = TimeUnit.builder()
                .reservation(reservation)
                .timeType(reservationRequestDTO.getTimeUnit()) // String -> Enum 변환
                .build();

        reservation.setTimeUnit(timeUnit);

        Reservation saved = reservationRepository.save(reservation);

        return ReservationResponseDTO.from(saved);
    }
}
