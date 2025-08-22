package coffeandcommit.crema.global.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@Getter
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {

        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("JWT secret key cannot be null or empty");
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to decode JWT secret key: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT secret key", e);
        }

        this.accessTokenValidityInMilliseconds = accessTokenExpiration;
        this.refreshTokenValidityInMilliseconds = refreshTokenExpiration;

        log.info("JWT Token Provider initialized - Access token validity: {}ms, Refresh token validity: {}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        try {
            String token = Jwts.builder()
                    .setSubject(userId)
                    .claim("type", "access")
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();

            log.debug("Access token created for user: {}", userId);
            return token;
        } catch (Exception e) {
            log.error("Failed to create access token for user: {} - {}", userId, e.getMessage());
            throw new RuntimeException("토큰 생성에 실패했습니다.", e);
        }
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        try {
            String token = Jwts.builder()
                    .setSubject(userId)
                    .claim("type", "refresh")
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();

            log.debug("Refresh token created for user: {}", userId);
            return token;
        } catch (Exception e) {
            log.error("Failed to create refresh token for user: {} - {}", userId, e.getMessage());
            throw new RuntimeException("리프레시 토큰 생성에 실패했습니다.", e);
        }
    }

    /**
     * 토큰에서 사용자명 추출
     */
    public String getUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            log.debug("Failed to extract userId from token: {}", e.getMessage());
            throw new IllegalArgumentException("토큰에서 사용자명 추출에 실패했습니다.", e);
        }
    }

    /**
     * 토큰에서 역할 추출 (더 이상 사용하지 않음 - 삭제 예정)
     */
    @Deprecated
    public String getRole(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("role", String.class);
        } catch (JwtException e) {
            log.debug("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 타입 확인 (access, refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("type", String.class);
        } catch (JwtException e) {
            log.debug("Failed to extract token type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 만료 시간 확인
     */
    public Date getExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (JwtException e) {
            log.debug("Failed to extract expiration from token: {}", e.getMessage());
            throw new IllegalArgumentException("토큰에서 만료 시간 추출에 실패했습니다.", e);
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.debug("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT token compact of handler are invalid: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token인지 확인
     */
    public boolean isAccessToken(String token) {
        String tokenType = getTokenType(token);
        return "access".equals(tokenType);
    }

    /**
     * Refresh Token인지 확인
     */
    public boolean isRefreshToken(String token) {
        String tokenType = getTokenType(token);
        return "refresh".equals(tokenType);
    }

    /**
     * 토큰 만료까지 남은 시간 (밀리초)
     */
    public long getRemainingTime(String token) {
        try {
            Date expiration = getExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}