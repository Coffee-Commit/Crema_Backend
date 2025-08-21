package coffeandcommit.crema.global.common.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Response<T> {
    private String message;
    private T data;
}