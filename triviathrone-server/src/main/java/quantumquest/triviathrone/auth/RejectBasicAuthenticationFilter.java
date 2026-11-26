package quantumquest.triviathrone.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects HTTP Basic credentials because authentication is bearer-token based.
 *
 * @author TiJ
 * @since 2.3.6
 */
public class RejectBasicAuthenticationFilter extends OncePerRequestFilter {
    /**
     * Rejects Basic authentication before any other authentication mechanism processes the request.
     *
     * @param request     the incoming request
     * @param response    the response used for authentication failures
     * @param filterChain the remaining HTTP filter chain
     * @throws ServletException if a downstream filter fails
     * @throws IOException      if the response or downstream filter cannot be written
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Basic authentication is not supported");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
