package coffeandcommit.crema.global.auth.jwt;

import coffeandcommit.crema.global.common.exception.BaseException;
import coffeandcommit.crema.global.common.exception.code.ErrorStatus;
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
import java.util.UUID;

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

            // HS512 알고리즘을 위한 최소 키 길이 검증 (512비트 = 64바이트)
            if (keyBytes.length < 64) {
                throw new IllegalArgumentException(
                        String.format("JWT secret key size is %d bits, but HS512 requires at least 512 bits (64 bytes). " +
                                        "Current key size: %d bytes. Please use a longer secret key.",
                                keyBytes.length * 8, keyBytes.length));
            }

            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to decode JWT secret key: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT secret key: " + e.getMessage(), e);
        }

        this.accessTokenValidityInMilliseconds = accessTokenExpiration;
        this.refreshTokenValidityInMilliseconds = refreshTokenExpiration;

        log.info("JWT Token Provider initialized - Access token validity: {}ms, Refresh token validity: {}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    public String createAccessToken(String memberId) {
        if (!StringUtils.hasText(memberId)) {
            throw new BaseException(ErrorStatus.BAD_REQUEST);
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        try {
            String token = Jwts.builder()
                    .setSubject(memberId)
                    .claim("type", "access")
                    .setId(UUID.randomUUID().toString()) // jti 클레임 추가
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();

            log.debug("Access token created for member: {}", memberId);
            return token;
        } catch (Exception e) {
            log.error("Failed to create access token for member: {} - {}", memberId, e.getMessage());
            throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String createRefreshToken(String memberId) {
        if (!StringUtils.hasText(memberId)) {
            throw new BaseException(ErrorStatus.BAD_REQUEST);
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        try {
            String token = Jwts.builder()
                    .setSubject(memberId)
                    .claim("type", "refresh")
                    .setId(UUID.randomUUID().toString()) // jti 클레임 추가
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .compact();

            log.debug("Refresh token created for member: {}", memberId);
            return token;
        } catch (Exception e) {
            log.error("Failed to create refresh token for member: {} - {}", memberId, e.getMessage());
            throw new BaseException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String getMemberId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            log.debug("Failed to extract memberId from token: {}", e.getMessage());
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }
    }

    // Spring Security와 호환성을 위해 getUsername 메소드 유지 (내부적으로 getMemberId 호출)
    public String getUsername(String token) {
        return getMemberId(token);
    }

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
     * 토큰의 jti(JWT ID) 클레임 추출
     */
    public String getJti(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getId();
        } catch (JwtException e) {
            log.debug("Failed to extract jti from token: {}", e.getMessage());
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }
    }

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
            throw new BaseException(ErrorStatus.INVALID_TOKEN);
        }
    }

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

    public boolean isAccessToken(String token) {
        String tokenType = getTokenType(token);
        return "access".equals(tokenType);
    }

    public boolean isRefreshToken(String token) {
        String tokenType = getTokenType(token);
        return "refresh".equals(tokenType);
    }

    public long getRemainingTime(String token) {
        try {
            Date expiration = getExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}