package com.starmitra.platform.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Authenticates STOMP CONNECT frames with the same JWT access token as REST —
 * WS sessions are principals carrying userId + system roles. Subscription-time
 * membership checks happen per-destination in M06 service layer.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WS CONNECT requires Bearer access token");
        }
        try {
            Jwt jwt = jwtDecoder.decode(auth.substring(7));
            accessor.setUser(new WsPrincipal(UUID.fromString(jwt.getSubject()), jwt.getClaimAsStringList("roles")));
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid access token");
        }
        return message;
    }

    public record WsPrincipal(UUID userId, List<String> roles) implements Principal {
        @Override public String getName() { return userId.toString(); }
    }
}
