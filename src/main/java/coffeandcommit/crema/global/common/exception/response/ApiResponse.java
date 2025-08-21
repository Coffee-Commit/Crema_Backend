package coffeandcommit.crema.global.common.exception.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import coffeandcommit.crema.global.common.exception.code.BaseCode;

@Getter
@JsonPropertyOrder({"isSuccess", "message", "result"})
public class ApiResponse<T> {

    private final boolean isSuccess;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    // BaseCode를 사용하는 생성자
    private ApiResponse(boolean isSuccess, BaseCode code, T result) {
        this.isSuccess = isSuccess;
        this.message = code.getMessage();
        this.result = result;
    }

    // String 메시지를 직접 사용하는 생성자
    private ApiResponse(boolean isSuccess, String message, T result) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.result = result;
    }

    public static <T> ApiResponse<T> onSuccess(BaseCode code, T result) {
        return new ApiResponse<>(true, code, result);
    }

    public static <T> ApiResponse<T> onFailure(BaseCode code, T result) {
        return new ApiResponse<>(false, code, result);
    }

    /**
     * BaseCustomException을 처리하기 위한 정적 팩토리 메서드
     * @param message 직접 사용할 메시지
     * @param result 결과 데이터 (주로 null)
     * @return 실패 ApiResponse 객체
     */
    public static <T> ApiResponse<T> onFailure(String message, T result) {
        // isSuccess, message, result를 받는 private 생성자 필요
        return new ApiResponse<>(false, message, result);
    }
}