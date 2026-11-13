package quantumquest.triviathrone.ws;

import quantumquest.triviathrone.auth.TokenService;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.auth.UserConfigLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtChannelInterceptorTest {
    private final TokenService tokens = mock(TokenService.class);
    private final UserConfigLoader users = mock(UserConfigLoader.class);
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(tokens, users);

    @Test
    @SuppressWarnings("unchecked")
    void rejectsWebSocketConnectionsWhenTheUserRoleHasBeenRemoved() {
        Claims claims = mock(Claims.class);
        Jws<Claims> parsedToken = mock(Jws.class);
        UserAccount account = new UserAccount();
        account.setUsername("player");
        account.setRoles(Set.of(UserAccount.ROLE_ADMIN));
        when(tokens.parse("signed-token")).thenReturn(parsedToken);
        when(parsedToken.getBody()).thenReturn(claims);
        when(claims.getSubject()).thenReturn("player");
        when(users.findByUsername("player")).thenReturn(account);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer signed-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, mock(MessageChannel.class)));
    }
}
