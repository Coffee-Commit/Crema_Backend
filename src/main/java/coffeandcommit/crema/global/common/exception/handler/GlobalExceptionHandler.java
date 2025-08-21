package coffeandcommit.crema.global.common.exception.handler;

import lombok.extern.slf4j.Slf4j;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.BaseCode;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.exception.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 어플리케이션 전역에서 발생하는 예외를 처리하는 핸들러.
 * BaseException + ErrorStatus 방식으로 통일된 예외 처리를 제공합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * BaseException을 처리합니다.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.warn("BaseException occurred: {}", e.getErrorCode().getMessage(), e);
        BaseCode code = e.getErrorCode();
        return createErrorResponse(code, null);
    }

    /**
     * @Valid 유효성 검사 실패 시 예외를 처리합니다.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Validation Exception occurred: {}", e.getBindingResult().getAllErrors());
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        BaseCode errorStatus = ErrorStatus.VALIDATION_ERROR;
        ApiResponse<Map<String, String>> body = ApiResponse.onFailure(errorStatus, errors);
        return super.handleExceptionInternal(
                e,
                body,
                headers,
                errorStatus.getHttpStatus(),
                request
        );
    }

    /**
     * Spring Security의 인가(권한) 예외를 처리합니다.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("AuthorizationDeniedException: {}", e.getMessage());
        BaseCode errorStatus = ErrorStatus.FORBIDDEN;
        return createErrorResponse(errorStatus, null);
    }

    /**
     * 위에서 처리되지 않은 모든 예외를 최종적으로 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        log.error("Unhandled exception occurred: {}", e.getMessage(), e);
        BaseCode errorStatus = ErrorStatus.INTERNAL_SERVER_ERROR;
        return createErrorResponse(errorStatus, null);
    }

    /**
     * BaseCode를 기반으로 실패 응답 ResponseEntity를 생성하는 유틸리티 메서드입니다.
     */
    private <T> ResponseEntity<ApiResponse<T>> createErrorResponse(BaseCode code, T data) {
        ApiResponse<T> response = ApiResponse.onFailure(code, data);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }
}