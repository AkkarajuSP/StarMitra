package com.starmitra.modules.connect.api;

import com.starmitra.modules.connect.application.ConnectService;
import com.starmitra.platform.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** /api/v1/conversations + /api/v1/users/me/blocks — M06. */
@RestController
public class ConnectController {

    private final ConnectService connect;

    public ConnectController(ConnectService connect) {
        this.connect = connect;
    }

    @GetMapping("/api/v1/conversations")
    public ResponseEntity<ConnectDtos.ConversationPage> listConversations(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = connect.list(SecurityUtils.currentUserId(), cursor, limit);
        return ResponseEntity.ok(new ConnectDtos.ConversationPage(
                p.items().stream().map(this::toConvDto).toList(),
                new ConnectDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @PostMapping("/api/v1/conversations")
    public ResponseEntity<ConnectDtos.Conversation> createConversation(
            @Valid @RequestBody ConnectDtos.ConversationCreate body) {
        var c = connect.create(SecurityUtils.currentUserId(),
                new ConnectService.CreateCommand(body.type(), body.participantIds(), body.projectId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toConvDto(c));
    }

    @GetMapping("/api/v1/conversations/{conversationId}")
    public ResponseEntity<ConnectDtos.Conversation> getConversation(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(toConvDto(connect.get(SecurityUtils.currentUserId(), conversationId)));
    }

    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    public ResponseEntity<ConnectDtos.MessagePage> listMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Long since,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var p = connect.history(SecurityUtils.currentUserId(), conversationId, since, cursor, limit);
        return ResponseEntity.ok(new ConnectDtos.MessagePage(
                p.items().stream().map(this::toMsgDto).toList(),
                new ConnectDtos.PageMeta(p.page().nextCursor(), p.page().hasMore(), p.page().total())));
    }

    @PostMapping("/api/v1/conversations/{conversationId}/messages")
    public ResponseEntity<ConnectDtos.Message> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConnectDtos.MessageSend body) {
        var m = connect.send(SecurityUtils.currentUserId(), conversationId,
                new ConnectService.SendCommand(body.body(), body.clientMessageId(), body.attachmentMediaIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toMsgDto(m));
    }

    @PostMapping("/api/v1/conversations/{conversationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID conversationId,
                                         @Valid @RequestBody ConnectDtos.ReadMark body) {
        connect.markRead(SecurityUtils.currentUserId(), conversationId, body.upToSequence());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/users/me/blocks/{userId}")
    public ResponseEntity<Void> blockUser(@PathVariable UUID userId) {
        connect.block(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/users/me/blocks/{userId}")
    public ResponseEntity<Void> unblockUser(@PathVariable UUID userId) {
        connect.unblock(SecurityUtils.currentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    private ConnectDtos.Conversation toConvDto(ConnectService.ConversationView c) {
        return new ConnectDtos.Conversation(c.id(), c.type(), c.projectId(), c.status());
    }

    private ConnectDtos.Message toMsgDto(ConnectService.MessageView m) {
        return new ConnectDtos.Message(m.id(), m.conversationId(), m.senderId(), m.sequence(),
                m.body(), m.sentAt(), m.clientMessageId(), m.attachmentMediaIds());
    }
}
