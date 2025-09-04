package coffeandcommit.crema.domain.guide.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum TimeType {

    MINUTE_30(30, 8000),
    MINUTE_60(60, 15000);

    private final int minutes;
    private final int price;

}
