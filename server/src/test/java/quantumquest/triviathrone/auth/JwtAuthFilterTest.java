package quantumquest.triviathrone.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {
    private final TokenService tokens = mock(TokenService.class);
    private final UserConfigLoader users = mock(UserConfigLoader.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(tokens, users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void takesAuthoritiesFromCurrentAccountRolesRatherThanTheToken() throws Exception {
        Claims claims = mock(Claims.class);
        Jws<Claims> parsedToken = mock(Jws.class);
        UserAccount account = new UserAccount();
        account.setUsername("player");
        account.setRoles(Set.of(UserAccount.ROLE_USER));
        when(tokens.parse("signed-token")).thenReturn(parsedToken);
        when(parsedToken.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("player");
        when(users.findByUsername("player")).thenReturn(account);

        authenticateRequest();
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));

        SecurityContextHolder.clearContext();
        account.setRoles(Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
        authenticateRequest();

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        verify(users, times(2)).findByUsername("player");
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsAccessTokensWhenTheUserRoleHasBeenRemoved() throws Exception {
        Claims claims = mock(Claims.class);
        Jws<Claims> parsedToken = mock(Jws.class);
        UserAccount account = new UserAccount();
        account.setUsername("player");
        account.setRoles(Set.of(UserAccount.ROLE_ADMIN));
        when(tokens.parse("signed-token")).thenReturn(parsedToken);
        when(parsedToken.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("player");
        when(users.findByUsername("player")).thenReturn(account);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-token");

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    private void authenticateRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-token");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));
    }
}
