package br.com.fiap.marketbuddies.api.configs;

import br.com.fiap.marketbuddies.api.controllers.handlers.ApiExceptionHandler;
import br.com.fiap.marketbuddies.api.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        http
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation(fixation -> fixation.none()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                        .ignoringRequestMatchers("/api/**"))
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers("/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/emails/**").hasRole("ADMIN")
                        .requestMatchers("/api/delivery-zones/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/checkout/**", "/orders/**").hasRole("CUSTOMER")
                        .requestMatchers("/", "/login", "/logout", "/signup", "/products/**", "/cart/**",
                                "/css/**", "/img/**", "/js/**", "/favicon.svg", "/error", "/403").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint(jsonMapper))
                        .accessDeniedHandler(accessDeniedHandler(jsonMapper)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint(JsonMapper jsonMapper) {
        return (request, response, authException) -> {
            if (isApi(request)) {
                writeJson(response, jsonMapper, HttpStatus.UNAUTHORIZED,
                        "Autenticação necessária: envie o token no header Authorization: Bearer", request);
                return;
            }
            String target = UrlUtils.buildRequestUrl(request);
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + UriUtils.encodeQueryParam(target, StandardCharsets.UTF_8));
        };
    }

    private AccessDeniedHandler accessDeniedHandler(JsonMapper jsonMapper) {
        AccessDeniedHandlerImpl page = new AccessDeniedHandlerImpl();
        page.setErrorPage("/403");
        return (request, response, accessDeniedException) -> {
            if (isApi(request)) {
                writeJson(response, jsonMapper, HttpStatus.FORBIDDEN,
                        "Acesso negado para o seu perfil", request);
                return;
            }
            page.handle(request, response, accessDeniedException);
        };
    }

    private static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private static void writeJson(HttpServletResponse response, JsonMapper jsonMapper, HttpStatus status,
                                  String message, HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(),
                ApiExceptionHandler.body(status, message, request.getRequestURI(), null));
    }
}
