package coffeandcommit.crema.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()  // 모든 요청 허용
                )
                .csrf(csrf -> csrf.disable())  // CSRF 완전 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS 설정 적용
                .headers(headers -> headers
                        .frameOptions().disable()  // H2 콘솔을 위한 프레임 옵션 비활성화
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");  // 모든 도메인 허용
        configuration.addAllowedMethod("*");  // 모든 HTTP 메소드 허용
        configuration.addAllowedHeader("*");  // 모든 헤더 허용
        configuration.setAllowCredentials(true);  // 인증 정보 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}