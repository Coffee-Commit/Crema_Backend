package coffeandcommit.crema.domain.reservation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum Status {

    PENDING,
    CONFIRMED,
    CANCELLED,
    REJECTED;

}
