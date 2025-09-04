package coffeandcommit.crema.domain.reservation.controller;

import coffeandcommit.crema.domain.reservation.dto.request.ReservationRequestDTO;
import coffeandcommit.crema.domain.reservation.dto.response.ReservationResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "reservation" , description = "예약(커피챗) API")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "커피챗 예약 신청", description = "커피챗 예약을 신청합니다.")
    @PostMapping
    public ResponseEntity<Response<ReservationResponseDTO>> createReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationRequestDTO reservationRequestDTO) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReservationResponseDTO result = reservationService.createReservation(loginMemberId, reservationRequestDTO);

        Response<ReservationResponseDTO> response = Response.<ReservationResponseDTO>builder()
                .message("커피챗 예약 신청 선공")
                .data(result)
                .build();

        return ResponseEntity.ok(response);
    }
}
