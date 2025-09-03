package coffeandcommit.crema.domain.review.controller;

import coffeandcommit.crema.domain.review.dto.request.ReviewRequestDTO;
import coffeandcommit.crema.domain.review.dto.response.ReviewResponseDTO;
import coffeandcommit.crema.domain.review.service.ReviewService;
import coffeandcommit.crema.global.auth.service.CustomUserDetails;
import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
import coffeandcommit.crema.global.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review" , description = "리뷰 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 등록", description = "리뷰를 등록합니다.")
    @PostMapping
    public ResponseEntity<Response<ReviewResponseDTO>> createReview(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ReviewRequestDTO reviewRequestDTO) {

        if(userDetails == null){
            throw new BaseException(ErrorStatus.UNAUTHORIZED);
        }

        String loginMemberId = userDetails.getMemberId();

        ReviewResponseDTO result = reviewService.createReview(loginMemberId, reviewRequestDTO);

        Response<ReviewResponseDTO> response = Response.<ReviewResponseDTO>builder()
                .message("리뷰 등록 완료")
                .data(result)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);


    }

}
