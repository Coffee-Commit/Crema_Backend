package coffeandcommit.crema.global.auth.service;

import coffeandcommit.crema.global.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    /**
     * 토큰을 블랙리스트에 추가
     */
    public void blacklistToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        try {
            // 토큰의 남은 유효시간 계산
            long remainingTime = jwtTokenProvider.getRemainingTime(token);

            if (remainingTime > 0) {
                String key = BLACKLIST_PREFIX + token;

                // Redis에 토큰 저장 (TTL은 토큰의 남은 유효시간)
                redisTemplate.opsForValue().set(key, "blacklisted", remainingTime, TimeUnit.MILLISECONDS);

                log.info("Token blacklisted successfully, expires in {} ms", remainingTime);
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
            // 블랙리스트 실패해도 서비스 중단되지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String key = BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check token blacklist: {}", e.getMessage());
            // Redis 오류 시 보안상 false 반환 (토큰 검증은 JWT 자체 검증으로)
            return false;
        }
    }

    /**
     * 사용자의 모든 활성 토큰을 블랙리스트에 추가 (로그아웃 시 사용)
     * 현재 요청의 Access Token과 Refresh Token을 블랙리스트에 추가
     */
    public void blacklistUserTokens(String accessToken, String refreshToken) {
        if (accessToken != null) {
            blacklistToken(accessToken);
            log.info("Access token blacklisted for user logout");
        }

        if (refreshToken != null) {
            blacklistToken(refreshToken);
            log.info("Refresh token blacklisted for user logout");
        }
    }

    /**
     * 특정 패턴의 키들을 정리 (관리용, 필요시 사용)
     */
    public void cleanupExpiredTokens() {
        try {
            // Redis TTL에 의해 자동으로 만료되므로 별도 정리 불필요
            // 필요하다면 SCAN 명령어로 만료된 키들을 찾아서 삭제 가능
            log.debug("Token cleanup completed (TTL auto-expiry)");
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens: {}", e.getMessage());
        }
    }
}