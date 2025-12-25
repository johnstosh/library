/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private CustomOidcUserService customOidcUserService;

    @Autowired
    private DynamicClientRegistrationRepository dynamicClientRegistrationRepository;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/test-data/**").permitAll()
                        .requestMatchers("/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/photos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/authors", "/api/libraries").permitAll()
                        .requestMatchers("/apply/api/**").hasAuthority("LIBRARIAN")
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/user-settings").authenticated()
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/global-settings/sso-status").permitAll()
                        .requestMatchers("/api/global-properties/**").permitAll()
                        .requestMatchers("/api/photo-export/**").authenticated()
                        .requestMatchers("/api/oauth/google/authorize", "/api/oauth/google/callback").permitAll()
                        .requestMatchers("/api/import/**").hasAuthority("LIBRARIAN")
                        .requestMatchers("/api/auth/**", "/login", "/search", "/oauth2/**", "/css/**", "/js/**", "/images/**", "/assets/**", "/vite.svg", "/", "/index.html", "/favicon.ico", "/apply-for-card.html", "/apply").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request,
                                                                HttpServletResponse response,
                                                                Authentication authentication) throws IOException {
                                // Don't redirect - just return 200 OK
                                // This avoids mixed content issues with HTTPS → HTTP redirects
                                response.setStatus(HttpServletResponse.SC_OK);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"success\":true}");
                                response.getWriter().flush();
                            }
                        })
                        .failureUrl("/?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/")
                        .clientRegistrationRepository(dynamicClientRegistrationRepository)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request,
                                                                HttpServletResponse response,
                                                                Authentication authentication) throws IOException {
                                // Redirect to home page after successful OAuth2 login
                                logger.info("OAuth2 login successful for: {}", authentication.getName());
                                response.sendRedirect("/");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            logger.error("OAuth2 login failed", exception);
                            logger.error("Exception type: {}", exception.getClass().getName());
                            logger.error("Exception message: {}", exception.getMessage());
                            if (exception.getCause() != null) {
                                logger.error("Cause: {}", exception.getCause().getMessage());
                            }
                            response.sendRedirect("/?error=true");
                        })
                )
                .logout(logout -> logout.permitAll());
        return http.build();
    }
}
