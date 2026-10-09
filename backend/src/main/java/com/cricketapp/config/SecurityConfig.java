package com.cricketapp.config;

import com.cricketapp.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {}) // Uses WebMvcConfigurer CorsConfig
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/request-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/verify-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register/resend-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password/request-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password/verify-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/players/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/teams/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/matches/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/matches/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/leaderboard", "/api/public/leaderboard/**").permitAll()
                        .requestMatchers("/ws-cricket/**").permitAll()
                        .requestMatchers("/api/dev/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
