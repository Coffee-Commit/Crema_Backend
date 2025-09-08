package coffeandcommit.crema.global.auth.config;

import coffeandcommit.crema.global.auth.handler.OAuth2AuthenticationFailureHandler;
import coffeandcommit.crema.global.auth.handler.OAuth2AuthenticationSuccessHandler;
import coffeandcommit.crema.global.auth.jwt.JwtAuthenticationEntryPoint;
import coffeandcommit.crema.global.auth.jwt.JwtAuthenticationFilter;
import coffeandcommit.crema.global.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .securityContext(context -> context.requireExplicitSave(false))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Prometheus Actuator Endpoint
                        .requestMatchers(EndpointRequest.to("prometheus")).permitAll()

                        // OAuth2 관련 URL들 - 인증 불필요
                        .requestMatchers(
                                "/oauth2/**",           // OAuth2 기본 경로
                                "/api/oauth2/**",       // 커스텀 OAuth2 경로
                                "/login/oauth2/**",     // OAuth2 로그인 콜백
                                "/api/login/oauth2/**"  // 커스텀 OAuth2 콜백
                        ).permitAll()

                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/status",
                                "/api/auth/refresh",
                                "/api/member/check/**",
                                "/api/test/auth/**",
                                "/api/debug/**",
                                // Swagger UI
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                // Health check
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Auth endpoints (인증 필요)
                        .requestMatchers("/api/auth/**").authenticated()

                        // Member endpoints (인증 필요)
                        .requestMatchers("/api/member/**").authenticated()
                        .requestMatchers("/api/images/**").authenticated()

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/api/oauth2/authorization")  // 프론트엔드 요청 URL과 맞춤
                        )
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/api/login/oauth2/code/*")   // 콜백 URL 설정
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );

        // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                // 로컬 개발 환경
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:8080",
                // dev 서버 환경 (API 서버)
                "https://dev-api-coffeechat.kro.kr",
                // dev 서버 환경 (프론트엔드 서버)
                "https://dev-coffeechat.kro.kr",
                "https://dev.coffeechat.kro.kr",
                // 프로덕션 환경
                "https://coffeechat.kro.kr",
                "https://api.coffeechat.kro.kr",
                // 모든 coffeechat.kro.kr 서브도메인 허용
                "https://*.coffeechat.kro.kr"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}