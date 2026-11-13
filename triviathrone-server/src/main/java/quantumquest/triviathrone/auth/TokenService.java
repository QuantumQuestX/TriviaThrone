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

@Service
public class TokenService {

    private final SecretKey key;
    private final long validitySeconds;

    public TokenService(AuthProperties props) {
        AuthProperties properties = Objects.requireNonNull(props, "props");
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.tokenValiditySeconds();
    }

    public String issueToken(UserAccount user) {
        Objects.requireNonNull(user, "user");
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getRoles())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        Objects.requireNonNull(token, "token");
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public long getValiditySeconds() {
        return validitySeconds;
    }
}
