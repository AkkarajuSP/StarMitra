package com.starmitra.modules.connect.application;

import com.starmitra.platform.websocket.WebSocketAuthChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * STOMP SUBSCRIBE authorization — every `/topic/conversations/{id}`
 * subscription is membership-checked via the same rule as REST history.
 * Non-member subscribe is rejected (no existence leak).
 */
@Component
public class ConversationSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern DEST =
            Pattern.compile("^/topic/conversations/([0-9a-fA-F-]{36})$");

    private final ConnectService connect;

    public ConversationSubscriptionInterceptor(ConnectService connect) {
        this.connect = connect;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        var matcher = DEST.matcher(String.valueOf(accessor.getDestination()));
        if (!matcher.matches()) {
            return message;
        }
        var principal = accessor.getUser();
        if (!(principal instanceof WebSocketAuthChannelInterceptor.WsPrincipal p)
                || !connect.canAccess(p.userId(), UUID.fromString(matcher.group(1)))) {
            throw new IllegalArgumentException("Not authorized to subscribe");
        }
        return message;
    }
}
