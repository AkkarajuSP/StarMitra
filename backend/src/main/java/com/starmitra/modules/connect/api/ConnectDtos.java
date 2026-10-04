package com.starmitra.modules.connect.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class ConnectDtos {

    private ConnectDtos() {}

    public record Conversation(UUID id, String type, UUID projectId, String status) {}

    public record ConversationCreate(
            @NotBlank String type,
            List<UUID> participantIds,
            UUID projectId) {}

    public record PageMeta(String nextCursor, boolean hasMore, Integer total) {}
    public record ConversationPage(List<Conversation> items, PageMeta page) {}

    public record Message(UUID id, UUID conversationId, UUID senderId, long sequence,
                          String body, String sentAt, String clientMessageId,
                          List<UUID> attachmentMediaIds) {}

    public record MessageSend(
            @Size(max = 4000) String body,
            @NotBlank @Size(max = 128) String clientMessageId,
            List<UUID> attachmentMediaIds) {}

    public record MessagePage(List<Message> items, PageMeta page) {}

    public record ReadMark(@NotNull @PositiveOrZero Long upToSequence) {}
}
