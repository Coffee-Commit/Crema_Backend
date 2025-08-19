# 새로운 예외처리 시스템 사용법

## 📖 개요

Crema 프로젝트에서는 일관된 API 응답과 체계적인 예외 관리를 위해 새로운 예외처리 시스템을 도입했습니다.

## 🏗️ 시스템 구조

### 1. 핵심 컴포넌트

- **BaseException**: 모든 커스텀 예외의 부모 클래스
- **BaseCode**: 상태 코드 인터페이스
- **ErrorStatus**: 오류 상태 코드 enum
- **SuccessStatus**: 성공 상태 코드 enum
- **ApiResponse<T>**: 통합 응답 DTO
- **GlobalExceptionHandler**: 글로벌 예외 처리기

### 2. 패키지 구조

```
global/common/exception/
├── BaseException.java
├── code/
│   ├── BaseCode.java
│   ├── ErrorStatus.java
│   └── SuccessStatus.java
├── response/
│   └── ApiResponse.java
└── handler/
    └── GlobalExceptionHandler.java
```

## 🚀 사용법

### 1. 새로운 예외 정의하기

#### Step 1: ErrorStatus에 상태 코드 추가

```java
// ErrorStatus.java
public enum ErrorStatus implements BaseCode {
    // 기존 코드들...
    
    // 새로운 도메인 예외 추가
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "상품이 품절되었습니다.");
}
```

#### Step 2: 커스텀 예외 클래스 생성

```java
// ProductNotFoundException.java
package coffeandcommit.crema.domain.product.exception;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;

public class ProductNotFoundException extends BaseException {
    public ProductNotFoundException() {
        super(ErrorStatus.PRODUCT_NOT_FOUND);
    }
}
```

### 2. 예외 발생시키기

#### 서비스 레이어에서 예외 발생

```java
// ProductService.java
@Service
public class ProductService {
    
    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException());
    }
    
    public void purchaseProduct(Long productId, int quantity) {
        Product product = findById(productId);
        
        if (product.getStock() < quantity) {
            throw new ProductOutOfStockException();
        }
        
        // 구매 로직...
    }
}
```

### 3. 성공 응답 처리

#### 컨트롤러에서 성공 응답 반환

```java
// ProductController.java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProduct(id);
        
        ApiResponse<ProductResponse> response = ApiResponse.onSuccess(
            SuccessStatus.OK, 
            product
        );
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        
        ApiResponse<ProductResponse> response = ApiResponse.onSuccess(
            SuccessStatus.CREATED, 
            product
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

## 📋 응답 형식

### 성공 응답

```json
{
    "isSuccess": true,
    "message": "[SUCCESS]성공입니다.",
    "result": {
        "id": 1,
        "name": "상품명",
        "price": 10000
    }
}
```

### 실패 응답

```json
{
    "isSuccess": false,
    "message": "[ERROR]상품을 찾을 수 없습니다.",
    "result": null
}
```

### 유효성 검사 실패 응답

```json
{
    "isSuccess": false,
    "message": "[ERROR]유효성 검사 실패",
    "result": {
        "name": "상품명은 필수입니다.",
        "price": "가격은 0보다 커야 합니다."
    }
}
```

## ⚙️ GlobalExceptionHandler 동작 방식

### 처리되는 예외 타입

1. **BaseException**: 커스텀 예외
2. **MethodArgumentNotValidException**: @Valid 유효성 검사 실패
3. **AuthorizationDeniedException**: 권한 없음
4. **Exception**: 처리되지 않은 모든 예외

### 예외 처리 흐름

```
1. 예외 발생
   ↓
2. GlobalExceptionHandler가 예외 타입 확인
   ↓
3. 해당하는 @ExceptionHandler 메서드 실행
   ↓
4. 로그 기록
   ↓
5. 일관된 ApiResponse 형식으로 응답 반환
```

## 🔧 기존 코드에서 마이그레이션

### 기존 방식 (레거시)

```java
// ❌ 기존 방식 - 더 이상 사용하지 않음
public class OldException extends BaseCustomException {
    public OldException() {
        super(HttpStatus.BAD_REQUEST, "오류 메시지", 400);
    }
}
```

### 새로운 방식

```java
// ✅ 새로운 방식
// 1. ErrorStatus에 상태 추가
OLD_ERROR(HttpStatus.BAD_REQUEST, "오류 메시지"),

// 2. 예외 클래스 수정
public class NewException extends BaseException {
    public NewException() {
        super(ErrorStatus.OLD_ERROR);
    }
}
```

## 📝 주의사항

1. **일관성**: 모든 새로운 예외는 반드시 BaseException을 상속해야 합니다.
2. **ErrorStatus 우선**: 하드코딩된 메시지 대신 ErrorStatus enum을 사용하세요.
3. **로깅**: GlobalExceptionHandler가 자동으로 로그를 기록하므로 중복 로깅을 피하세요.
4. **응답 형식**: 직접 ResponseEntity를 구성하지 말고 ApiResponse를 사용하세요.

## 🎯 모범 사례

### DO ✅

```java
// 명확하고 구체적인 예외명
public class EmailAlreadyExistsException extends BaseException {
    public EmailAlreadyExistsException() {
        super(ErrorStatus.EMAIL_DUPLICATED);
    }
}

// 도메인별로 ErrorStatus 그룹화
// User Domain
USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
USER_EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),

// Product Domain  
PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "상품이 품절되었습니다."),
```

### DON'T ❌

```java
// 너무 일반적인 예외명
public class BadException extends BaseException { ... }

// 하드코딩된 메시지
throw new RuntimeException("사용자를 찾을 수 없습니다.");

// 직접 ResponseEntity 구성
return ResponseEntity.badRequest().body("오류 발생");
```

---

이 문서는 Crema 프로젝트의 예외처리 시스템을 이해하고 올바르게 사용하기 위한 가이드입니다. 
추가 질문이나 개선사항이 있다면 팀에 문의해주세요.