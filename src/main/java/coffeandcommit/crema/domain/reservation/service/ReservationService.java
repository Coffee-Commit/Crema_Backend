package coffeandcommit.crema.domain.reservation.service;

import coffeandcommit.crema.domain.guide.entity.Guide;
import coffeandcommit.crema.domain.guide.entity.TimeUnit;
import coffeandcommit.crema.domain.guide.repository.GuideRepository;
import coffeandcommit.crema.domain.member.entity.Member;
import coffeandcommit.crema.domain.member.repository.MemberRepository;
import coffeandcommit.crema.domain.reservation.dto.request.ReservationDecisionRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.request.ReservationRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationDecisionResponseDTO;
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

    /* 커피챗 예약 승인/거절 */
    @Transactional
    public ReservationDecisionResponseDTO decideReservation(String loginMemberId, Long reservationId, @Valid ReservationDecisionRequestDTO reservationDecisionRequestDTO) {

        // 1. 예약 조회
        Reservation reservation = getReservationOrThrow(reservationId);

        // 2. 이미 처리된 예약인지 확인 (PENDING만 허용)
        if (reservation.getStatus() != Status.PENDING) {
            throw new BaseException(ErrorStatus.ALREADY_DECIDED);
        }

        // 3. 로그인한 사용자가 해당 예약의 가이드인지 확인
        if (!reservation.getGuide().getMember().getId().equals(loginMemberId)) {
            throw new BaseException(ErrorStatus.FORBIDDEN);
        }

        // 4. 상태 업데이트
        Status newStatus = reservationDecisionRequestDTO.getStatus();
        if (newStatus == Status.CONFIRMED) {
            reservation.setStatus(Status.CONFIRMED);

            int price = reservation.getTimeUnit().getTimeType().getPrice();

            // 멘티 포인트 차감
            Member mentee = reservation.getMember();
            try {
                mentee.decreasePoint(price);
            } catch (IllegalArgumentException e) {
                throw new BaseException(ErrorStatus.INSUFFICIENT_POINTS);
            }

            // 가이드 포인트 적립
            Member guideMember = reservation.getGuide().getMember();
            guideMember.addPoint(price);

        } else if (newStatus == Status.CANCELLED) {
            reservation.setStatus(Status.CANCELLED);
            reservation.setReason(reservationDecisionRequestDTO.getReason());

        } else {
            // CONFIRMED, CANCELLED 외 다른 상태는 요청 불가
            throw new BaseException(ErrorStatus.INVALID_STATUS);
        }

        return ReservationDecisionResponseDTO.from(reservation);
    }

}
