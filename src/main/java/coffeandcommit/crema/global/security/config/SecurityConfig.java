package coffeandcommit.crema.global.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
// @TODO: 아래 클래스들을 구현해야 합니다
// import coffeandcommit.crema.global.infra.auth.jwt.JwtAuthenticationEntryPoint;
// import coffeandcommit.crema.global.infra.auth.jwt.JwtAuthenticationFilter;
// import coffeandcommit.crema.global.infra.auth.jwt.JwtTokenProvider;
// import coffeandcommit.crema.global.infra.auth.service.RedisSessionService;
// import coffeandcommit.crema.global.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // @TODO: 다음 의존성들을 구현 후 주석 해제
    // private final JwtTokenProvider jwtProvider;
    // private final RedisSessionService redisSessionService;
    // private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    // private final ObjectMapper objectMapper;
    // private final CustomUserDetailsService customUserDetailsService;

    /*
        /api/login, /api/register, /api/refresh, 스웨거 경로 -> 인증 불필요
        h2-console -> 개발용 DB 콘솔 접근 허용
        나머지 /api/** -> 향후 인증 구현 시 로그인 필요
    */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults()) // CORS 활성화 (WebMvcConfigurer의 설정 사용)
                .csrf(AbstractHttpConfigurer::disable) // JWT 기반 인증은 브라우저 세션을 사용하지 않으므로 CSRF 불필요
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // STATELESS
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/login", "/api/register", "/api/refresh",
                                "/profile-images/**", "/product-images/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/h2-console/**",
                                "/api/v1/recruitments/scrape"
                        )
                        .permitAll()
                        // @TODO: Role 기반 권한 검사는 User 엔티티와 권한 시스템 구현 후 활성화
                        // .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // .requestMatchers("/api/v1/**", "/api/logout").hasAnyRole("ADMIN", "USER", "LECTURER")
                        .anyRequest().permitAll() // @TODO: 향후 .authenticated()로 변경
                )
                .httpBasic(AbstractHttpConfigurer::disable) // httpBasic 제거 또는 비활성화
                .logout(AbstractHttpConfigurer::disable)
                // @TODO: JWT 필터 체인 구현 후 활성화
                // .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, redisSessionService, objectMapper, customUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                // .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}