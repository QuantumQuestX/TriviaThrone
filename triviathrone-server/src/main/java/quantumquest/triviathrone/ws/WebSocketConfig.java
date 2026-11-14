package quantumquest.triviathrone.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Objects;

/**
 * Configures the STOMP broker, WebSocket endpoint, and inbound authentication.
 *
 * @author TiJ
 * @since 2.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    /**
     * Holds the jw hanne nterceptor used by this component.
     */
    private final JwtChannelInterceptor jwtChannelInterceptor;

    /**
     * Creates a WebSocketConfig.
     *
     * @param jwtChannelInterceptor the JWT channel interceptor
     */
    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor) {
        this.jwtChannelInterceptor = Objects.requireNonNull(jwtChannelInterceptor, "jwtChannelInterceptor");
    }

    /**
     * Configures application and broker message prefixes.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Installs JWT authentication on inbound STOMP messages.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }

    /**
     * Registers the SockJS STOMP endpoint.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/lobby")
            .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
