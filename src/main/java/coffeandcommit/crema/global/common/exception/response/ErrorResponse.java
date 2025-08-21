package coffeandcommit.crema.global.common.exception.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private int code;
    private String message;
}