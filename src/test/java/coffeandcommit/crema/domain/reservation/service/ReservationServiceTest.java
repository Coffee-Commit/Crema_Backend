package coffeandcommit.crema.domain.reservation.service;

import coffeandcommit.crema.domain.reservation.entity.Reservation;
import coffeandcommit.crema.domain.reservation.repository.ReservationRepository;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Reservation testReservation;
    private final Long RESERVATION_ID = 1L;

    @BeforeEach
    void setUp() {
        // Create a test reservation
        testReservation = Reservation.builder()
                .id(RESERVATION_ID)
                .build();
    }

    @Test
    @DisplayName("getReservationOrThrow - 성공 케이스: 예약이 존재하는 경우")
    void getReservationOrThrow_Success() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(testReservation));

        // When
        Reservation result = reservationService.getReservationOrThrow(RESERVATION_ID);

        // Then
        assertNotNull(result);
        assertEquals(RESERVATION_ID, result.getId());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getReservationOrThrow - 실패 케이스: 예약이 존재하지 않는 경우")
    void getReservationOrThrow_ReservationNotFound() {
        // Given
        when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> {
            reservationService.getReservationOrThrow(RESERVATION_ID);
        });

        assertEquals(ErrorStatus.RESERVATION_NOT_FOUND, exception.getErrorCode());
        verify(reservationRepository, times(1)).findById(RESERVATION_ID);
    }

    @Test
    @DisplayName("getReservationOrThrow - 실패 케이스: reservationId가 null인 경우")
    void getReservationOrThrow_NullReservationId() {
        // When & Then
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            reservationService.getReservationOrThrow(null);
        });
        assertEquals("reservationId must not be null", exception.getMessage());
        verify(reservationRepository, never()).findById(any());
    }
}
