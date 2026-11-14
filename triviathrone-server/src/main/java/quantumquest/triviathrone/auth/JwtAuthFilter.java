package quantumquest.triviathrone.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Authenticates HTTP bearer tokens before request authorization.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    /**
     * Holds the tokens used by this component.
     */
    private final TokenService tokens;
    /**
     * Holds the use onfi oader used by this component.
     */
    private final UserConfigLoader userConfigLoader;

    /**
     * Creates a JwtAuthFilter.
     *
     * @param tokens           the JWT service
     * @param userConfigLoader the user-account loader
     */
    public JwtAuthFilter(TokenService tokens, UserConfigLoader userConfigLoader) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.userConfigLoader = Objects.requireNonNull(userConfigLoader, "userConfigLoader");
    }

    /**
     * Authenticates a valid bearer token and continues the HTTP filter chain.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response used for authentication failures
     * @param filterChain the remaining filter chain
     * @throws ServletException if a downstream filter fails
     * @throws IOException      if the response or downstream filter cannot be written
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());

            try {
                Claims claims = tokens.parse(token).getBody();

                String username = claims.getSubject();
                UserAccount account = userConfigLoader.findByUsername(username);
                if (account == null || !account.isEnabled()
                    || !account.getRoles().contains(UserAccount.ROLE_USER)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
                    return;
                }

                List<SimpleGrantedAuthority> authorities = account.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
                Authentication auth = new UsernamePasswordAuthenticationToken(
                    account, null, authorities
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
