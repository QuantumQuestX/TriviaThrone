package quantumquest.triviathrone.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

/**
 * Issues and validates signed JWT access tokens.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Service
public class TokenService {

    /**
     * Holds the key used by this component.
     */
    private final SecretKey key;
    /**
     * Holds the validit econds used by this component.
     */
    private final long validitySeconds;

    /**
     * Creates a TokenService.
     *
     * @param props the configuration properties
     */
    public TokenService(AuthProperties props) {
        AuthProperties properties = Objects.requireNonNull(props, "props");
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.tokenValiditySeconds();
    }

    /**
     * Builds a signed access token containing the user's identity and roles.
     *
     * @param user the user account
     * @return the built a signed access token containing the user's identity and roles
     */
    public String issueToken(UserAccount user) {
        Objects.requireNonNull(user, "user");
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(user.getUsername())
            .claim("roles", user.getRoles())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(validitySeconds)))
            .signWith(key)
            .compact();
    }

    /**
     * Parses and verifies a signed access token.
     *
     * @param token the token
     * @return the parsed and verifies a signed access token
     */
    public Jws<Claims> parse(String token) {
        Objects.requireNonNull(token, "token");
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token);
    }

    /**
     * Returns the configured access-token lifetime.
     */
    public long getValiditySeconds() {
        return validitySeconds;
    }
}
