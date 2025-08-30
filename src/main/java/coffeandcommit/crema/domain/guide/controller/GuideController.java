package coffeandcommit.crema.domain.guide.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
@Tag(name = "Guide" , description = "가이드 API")
public class GuideController {
}
