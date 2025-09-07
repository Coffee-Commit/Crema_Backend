package coffeandcommit.crema.domain.reservation.controller;

import coffeandcommit.crema.domain.reservation.dto.request.ReservationDecisionRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.request.ReservationRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.response.*;
import coffeandcommit.crema.domain.reservation.service.ReservationService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation" , description = "예약(커피챗) API")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "커피챗 예약 신청", description = "커피챗 예약을 신청합니다.")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Response<ReservationResponseDTO>> createReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestPart("reservation") ReservationRequestDTO reservationRequestDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReservationResponseDTO result = reservationService.createReservation(loginMemberId, reservationRequestDTO, files);

        Response<ReservationResponseDTO> response = Response.<ReservationResponseDTO>builder()
                .message("커피챗 예약 신청 성공")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "커피챗 예약 승인/거절", description = "커피챗 예약을 승인 또는 거절합니다.")
    @PatchMapping("/{reservationId}")
    public ResponseEntity<Response<ReservationDecisionResponseDTO>> decideReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationDecisionRequestDTO reservationDecisionRequestDTO) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReservationDecisionResponseDTO result = reservationService.decideReservation(loginMemberId, reservationId, reservationDecisionRequestDTO);

        Response<ReservationDecisionResponseDTO> response = Response.<ReservationDecisionResponseDTO>builder()
                .message("커피챗 예약 승인/거절 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "커피챗 예약 신청 정보 조회", description = "특정 가이드의 커피챗 예약 신청 정보를 조회합니다.")
    @GetMapping("/apply/{guideId}")
    public ResponseEntity<Response<ReservationApplyResponseDTO>> getReservationApply(
            @PathVariable Long guideId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReservationApplyResponseDTO result = reservationService.getReservationApply(guideId, loginMemberId);

        Response<ReservationApplyResponseDTO> response = Response.<ReservationApplyResponseDTO>builder()
                .message("커피챗 예약 신청 정보 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);

    }

    @Operation(summary = "커피챗 사전 정보 조회", description = "특정 예약의 커피챗 사전 정보를 조회합니다.")
    @GetMapping("/{reservationId}/survey")
    public ResponseEntity<Response<ReservationSurveyResponseDTO>> getSurvey(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReservationSurveyResponseDTO result = reservationService.getSurvey(reservationId, loginMemberId);

        Response<ReservationSurveyResponseDTO> response = Response.<ReservationSurveyResponseDTO>builder()
                .message("사전 정보 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "나의 커피챗 요약 정보 조회", description = "로그인한 사용자의 커피챗 요약 정보를 조회합니다.")
    @GetMapping("/summary/me")
    public ResponseEntity<Response<CoffeeChatSummaryResponseDTO>> getMyCoffeeChatSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        CoffeeChatSummaryResponseDTO result = reservationService.getMyCoffeeChatSummary(loginMemberId);

        Response<CoffeeChatSummaryResponseDTO> response = Response.<CoffeeChatSummaryResponseDTO>builder()
                .message("나의 커피챗 요약 정보 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "커피챗 신청 완료 조회", description = "특정 예약의 신청 완료 페이지 정보를 조회합니다.")
    @GetMapping("/{reservationId}/completion")
    public ResponseEntity<Response<ReservationCompletionResponseDTO>> getReservationCompletion(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReservationCompletionResponseDTO result = reservationService.getReservationCompletion(loginMemberId, reservationId);

        Response<ReservationCompletionResponseDTO> response = Response.<ReservationCompletionResponseDTO>builder()
                .message("커피챗 신청 완료 조회 성공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }
}
