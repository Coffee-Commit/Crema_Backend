package coffeandcommit.crema.global.common.exception.code;

import org.springframework.http.HttpStatus;

/**
 * 모든 성공/오류 코드 Enum이 구현해야 할 최상위 인터페이스
 */
public interface BaseCode {
  HttpStatus getHttpStatus();
  String getMessage();
}
