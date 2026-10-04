package com.starmitra.modules.connect.application;

import com.starmitra.modules.connect.persistence.*;
import com.starmitra.modules.media.application.MediaReferenceContract;
import com.starmitra.modules.room.application.ProjectMembershipContract;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import com.starmitra.platform.pagination.Cursor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M06 — Connect. Owns conversations, conversation membership, messages,
 * receipts, attachments, user blocks. ADR-009: REST owns commands/history;
 * STOMP `/topic/conversations/{id}` carries realtime events after persist.
 *
 * Membership: ONE_TO_ONE/GROUP → M06 conversation_members. PROJECT → M07
 * ProjectMembershipContract (never duplicated). Blocks: user-level
 * (either direction) for ONE_TO_ONE create+send — not moderation.
 * clientMessageId → message_idempotency (PK) = at-least-once dedup.
 */
@Service
public class ConnectService {

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final MessageReceiptRepository receipts;
    private final MessageAttachmentRepository attachments;
    private final UserBlockRepository blocks;
    private final MessageIdempotencyRepository idempotency;
    private final ProjectMembershipContract projectMembers;
    private final MediaReferenceContract media;
    private final AuditService audit;
    private final SimpMessagingTemplate broker;

    public ConnectService(ConversationRepository conversations, ConversationMemberRepository members,
                          MessageRepository messages, MessageReceiptRepository receipts,
                          MessageAttachmentRepository attachments, UserBlockRepository blocks,
                          MessageIdempotencyRepository idempotency,
                          ProjectMembershipContract projectMembers, MediaReferenceContract media,
                          AuditService audit, SimpMessagingTemplate broker) {
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.receipts = receipts;
        this.attachments = attachments;
        this.blocks = blocks;
        this.idempotency = idempotency;
        this.projectMembers = projectMembers;
        this.media = media;
        this.audit = audit;
        this.broker = broker;
    }

    public record ConversationView(UUID id, String type, UUID projectId, String status) {}
    public record MessageView(UUID id, UUID conversationId, UUID senderId, long sequence,
                              String body, String sentAt, String clientMessageId,
                              List<UUID> attachmentMediaIds) {}
    public record Page(String nextCursor, boolean hasMore, Integer total) {}
    public record PageResult<T>(List<T> items, Page page) {}
    public record CreateCommand(String type, List<UUID> participantIds, UUID projectId) {}
    public record SendCommand(String body, String clientMessageId, List<UUID> attachmentMediaIds) {}

    // ---------- conversations ----------

    @Transactional
    public ConversationView create(UUID creator, CreateCommand cmd) {
        var type = parseType(cmd.type());
        if (type == ConversationEntity.Type.PROJECT) {
            if (cmd.projectId() == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "projectId required for PROJECT");
            }
            if (!projectMembers.isActiveMember(cmd.projectId(), creator)) {
                throw new ApiException(ErrorCode.NOT_A_MEMBER, "Not a project member");
            }
        }
        if (type == ConversationEntity.Type.ONE_TO_ONE) {
            if (cmd.participantIds() == null || cmd.participantIds().size() != 1) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "ONE_TO_ONE requires exactly 1 other participant");
            }
            var other = cmd.participantIds().get(0);
            if (other.equals(creator) || blocks.existsBetween(creator, other)) {
                throw new ApiException(ErrorCode.BLOCKED, "Cannot create conversation");
            }
        }
        var conv = conversations.save(new ConversationEntity(type, cmd.projectId(), creator));
        members.save(new ConversationMemberEntity(conv.getId(), creator, "OWNER"));
        if (type != ConversationEntity.Type.PROJECT && cmd.participantIds() != null) {
            for (var pid : cmd.participantIds()) {
                members.save(new ConversationMemberEntity(conv.getId(), pid, null));
            }
        }
        audit.record("M06", "CONVERSATION_CREATED", creator, "user",
                "conversation", conv.getId().toString(), type.name());
        return toConv(conv);
    }

    @Transactional(readOnly = true)
    public PageResult<ConversationView> list(UUID caller, String cursor, Integer limit) {
        int size = Cursor.limit(limit);
        int offset = offset(cursor);
        var rows = conversations.findMemberConversations(caller, PageRequest.of(0, size + 1 + offset));
        var window = rows.size() > offset ? rows.subList(offset, rows.size()) : List.<ConversationEntity>of();
        boolean hasMore = window.size() > size;
        return new PageResult<>(window.stream().limit(size).map(this::toConv).toList(),
                new Page(hasMore ? Cursor.encode("o", String.valueOf(offset + size)) : null, hasMore, null));
    }

    @Transactional(readOnly = true)
    public ConversationView get(UUID caller, UUID convId) {
        return toConv(requireAccess(caller, convId));
    }

    // ---------- messages ----------

    /**
     * Persisted send — conversation row pessimistically locked so sequence
     * assignment is serialized (uq_messages_conv_seq). clientMessageId dedup
     * via message_idempotency PK; attachments via M04 contract; broadcast
     * after commit → at-least-once, REST history is the recovery path.
     */
    @Transactional
    public MessageView send(UUID sender, UUID convId, SendCommand cmd) {
        if (cmd.clientMessageId() == null || cmd.clientMessageId().isBlank()
                || cmd.clientMessageId().length() > 128) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "clientMessageId required");
        }
        // dedup — replay returns original message
        var prior = idempotency.findById(cmd.clientMessageId());
        if (prior.isPresent()) {
            var msg = messages.findById(prior.get().getMessageId()).orElseThrow();
            return toMsg(msg, cmd.clientMessageId());
        }
        var conv = conversations.findByIdForUpdate(convId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireAccess(sender, conv);

        if (conv.getType() == ConversationEntity.Type.ONE_TO_ONE) {
            var others = members.findActiveUserIds(convId).stream()
                    .filter(u -> !u.equals(sender)).toList();
            if (others.size() == 1 && blocks.existsBetween(sender, others.get(0))) {
                throw new ApiException(ErrorCode.BLOCKED, "Blocked");
            }
        }
        for (UUID mediaId : cmd.attachmentMediaIds() == null ? List.<UUID>of() : cmd.attachmentMediaIds()) {
            if (!media.isUsableBy(mediaId, sender)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Attachment media not usable");
            }
        }
        long seq = messages.maxSequence(convId) + 1;
        var msg = messages.saveAndFlush(new MessageEntity(convId, sender, seq, cmd.body()));
        try {
            idempotency.saveAndFlush(new MessageIdempotencyEntity(cmd.clientMessageId(), msg.getId(), sender));
        } catch (DataIntegrityViolationException dup) {
            var winner = idempotency.findById(cmd.clientMessageId()).orElseThrow();
            return toMsg(messages.findById(winner.getMessageId()).orElseThrow(), cmd.clientMessageId());
        }
        if (cmd.attachmentMediaIds() != null) {
            int i = 0;
            for (UUID mediaId : cmd.attachmentMediaIds()) {
                attachments.save(new MessageAttachmentEntity(msg.getId(), mediaId, i++));
            }
        }
        for (var uid : members.findActiveUserIds(convId)) {
            if (!uid.equals(sender)) {
                receipts.save(new MessageReceiptEntity(msg.getId(), uid, MessageReceiptEntity.Status.DELIVERED));
            }
        }
        var view = toMsg(msg, cmd.clientMessageId());
        broadcast(convId, view);                                     // ADR-009 event
        return view;
    }

    /** History + reconnect recovery — `since` = last sequence client holds. */
    @Transactional(readOnly = true)
    public PageResult<MessageView> history(UUID caller, UUID convId, Long since,
                                           String cursor, Integer limit) {
        requireAccess(caller, convId);
        int size = Cursor.limit(limit);
        long afterSeq = since != null ? since : (cursor != null ? parseSeqCursor(cursor) : 0);
        var rows = messages.findSince(convId, afterSeq, PageRequest.of(0, size + 1));
        boolean hasMore = rows.size() > size;
        var items = rows.stream().limit(size).map(m -> toMsg(m, null)).toList();
        String next = hasMore && !items.isEmpty()
                ? Cursor.encode("s", String.valueOf(items.get(items.size() - 1).sequence()))
                : null;
        return new PageResult<>(items, new Page(next, hasMore, null));
    }

    @Transactional
    public void markRead(UUID caller, UUID convId, long upToSequence) {
        requireAccess(caller, convId);
        receipts.markReadUpTo(convId, upToSequence, caller);
    }

    // ---------- blocks ----------

    @Transactional
    public void block(UUID blocker, UUID blocked) {
        if (blocker.equals(blocked)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Cannot block yourself");
        }
        if (!blocks.existsById(new UserBlockEntity.Pk(blocker, blocked))) {
            try {
                blocks.saveAndFlush(new UserBlockEntity(blocker, blocked));
                audit.record("M06", "USER_BLOCKED", blocker, "user", "user", blocked.toString(), null);
            } catch (DataIntegrityViolationException dup) { /* already blocked */ }
        }
    }

    @Transactional
    public void unblock(UUID blocker, UUID blocked) {
        var key = new UserBlockEntity.Pk(blocker, blocked);
        if (blocks.existsById(key)) blocks.deleteById(key);
    }

    // ---------- WS subscription authz (SUBSCRIBE interceptor) ----------

    /** Membership check for STOMP SUBSCRIBE — same rule as REST reads. */
    @Transactional(readOnly = true)
    public boolean canAccess(UUID userId, UUID convId) {
        try {
            requireAccess(userId, convId);
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

    // ---------- internals ----------

    private ConversationEntity requireAccess(UUID caller, UUID convId) {
        var conv = conversations.findById(convId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireAccess(caller, conv);
        return conv;
    }

    /** Membership: PROJECT→M07 contract; others→conversation_members. */
    private void requireAccess(UUID caller, ConversationEntity conv) {
        boolean ok = conv.getType() == ConversationEntity.Type.PROJECT
                ? projectMembers.isActiveMember(conv.getProjectId(), caller)
                : members.findById(new ConversationMemberEntity.Pk(conv.getId(), caller))
                    .map(ConversationMemberEntity::isActive).orElse(false);
        if (!ok) throw new ApiException(ErrorCode.NOT_FOUND);   // hidden, not 403-leaked
    }

    private void broadcast(UUID convId, MessageView view) {
        broker.convertAndSend("/topic/conversations/" + convId,
                Map.of("type", "MESSAGE", "message", view));
    }

    private ConversationEntity.Type parseType(String t) {
        try {
            return ConversationEntity.Type.valueOf(t);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown conversation type");
        }
    }

    private ConversationView toConv(ConversationEntity c) {
        return new ConversationView(c.getId(), c.getType().name(), c.getProjectId(), c.getStatus());
    }

    private MessageView toMsg(MessageEntity m, String clientMessageId) {
        return new MessageView(m.getId(), m.getConversationId(), m.getSenderId(), m.getSequence(),
                m.getBody(), m.getSentAt().toString(), clientMessageId,
                attachments.findMediaIds(m.getId()));
    }

    private int offset(String cursor) {
        if (cursor == null) return 0;
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Integer.parseInt(p[1]) : 0;
    }

    private long parseSeqCursor(String cursor) {
        String[] p = Cursor.decode(cursor);
        return p.length == 2 ? Long.parseLong(p[1]) : 0;
    }
}
