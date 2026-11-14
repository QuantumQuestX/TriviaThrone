package quantumquest.triviathrone.ws;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import quantumquest.triviathrone.auth.TokenService;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.auth.UserConfigLoader;

import java.util.Objects;

/**
 * Authenticates STOMP CONNECT frames with bearer JWTs.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {
    /**
     * Holds the toke ervice used by this component.
     */
    private final TokenService tokenService;
    /**
     * Holds the use onfi oader used by this component.
     */
    private final UserConfigLoader userConfigLoader;

    /**
     * Creates a JwtChannelInterceptor.
     *
     * @param tokenService     the JWT service
     * @param userConfigLoader the user-account loader
     */
    public JwtChannelInterceptor(TokenService tokenService, UserConfigLoader userConfigLoader) {
        this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
        this.userConfigLoader = Objects.requireNonNull(userConfigLoader, "userConfigLoader");
    }

    /**
     * Authenticates an inbound STOMP CONNECT message.
     */
    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                throw new IllegalArgumentException("No token provided");
            }

            token = token.substring("Bearer ".length());
            try {
                Jws<Claims> claims = tokenService.parse(token);
                String username = claims.getBody().getSubject();
                UserAccount user = userConfigLoader.findByUsername(username);

                if (user == null || !user.isEnabled()
                        || !user.getRoles().contains(UserAccount.ROLE_USER)) {
                    throw new IllegalArgumentException("Invalid user");
                }

                accessor.setUser(user::getUsername);
            } catch (JwtException e) {
                throw new IllegalArgumentException("Invalid token");
            }
        }

        return message;
    }
}
