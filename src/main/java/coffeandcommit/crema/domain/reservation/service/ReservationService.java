package coffeandcommit.crema.domain.reservation.service;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public Reservation getReservationOrThrow(@NotNull Long reservationId) {

        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BaseException(ErrorStatus.RESERVATION_NOT_FOUND));
    }
}
