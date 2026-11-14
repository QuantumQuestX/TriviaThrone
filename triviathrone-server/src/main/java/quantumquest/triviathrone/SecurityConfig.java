package quantumquest.triviathrone;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import quantumquest.triviathrone.auth.AuthProperties;
import quantumquest.triviathrone.auth.JwtAuthFilter;

/**
 * Configures stateless JWT authentication and HTTP authorization.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    /**
     * Creates the password encoder used for stored account credentials.
     *
     * @return the created the password encoder used for stored account credentials
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Builds the stateless security filter chain and installs JWT authentication.
     *
     * @param http the HTTP security builder
     * @param jwt  the JWT authentication filter
     * @return the built the stateless security filter chain and installs JWT authentication
     * @throws Exception if security configuration cannot be built
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                .requestMatchers(
                    "/auth/login",
                    "/auth/refresh",
                    "/auth/logout",
                    "/api/version",
                    "/api/developers",
                    "/login",
                    "/login.html",
                    "/index.html",
                    "/admin",
                    "/admin.html",
                    "/lobby",
                    "/lobby.html",
                    "/game",
                    "/game-creator",
                    "/game-creator.html",
                    "/editor",
                    "/games/**",
                    "/mods/**",
                    "/plugins/**",
                    "/ws/**",
                    "/",
                    "/favicon.ico",
                    "/css/**",
                    "/js/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
