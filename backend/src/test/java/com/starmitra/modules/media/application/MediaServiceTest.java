package com.starmitra.modules.media.application;

import com.starmitra.modules.media.persistence.*;
import com.starmitra.platform.audit.AuditService;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaServiceTest {

    private MediaAssetRepository assets;
    private MediaVariantRepository variants;
    private ObjectStorageClient storage;
    private MediaProcessor processor;
    private MediaService service;
    private final UUID me = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assets = mock(MediaAssetRepository.class);
        variants = mock(MediaVariantRepository.class);
        storage = mock(ObjectStorageClient.class);
        processor = mock(MediaProcessor.class);
        service = new MediaService(assets, variants, storage, processor, mock(AuditService.class),
                (t, id) -> java.util.List.of(),   // no active M18 restrictions
                50_000_000L, Duration.ofMinutes(15), Duration.ofHours(1));
    }

    private MediaAssetEntity owned(MediaAssetEntity.Visibility vis) {
        var a = new MediaAssetEntity(me, me + "/k.jpg", "pic.jpg",
                MediaAssetEntity.MediaType.IMAGE, "image/jpeg", vis);
        when(assets.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void createValidatesMimeVsType() {
        var e = assertThrows(ApiException.class, () -> service.create(me,
                new MediaService.CreateCommand("IMAGE", "video/mp4", null, null, null)));
        assertEquals(ErrorCode.VALIDATION_FAILED, e.code());
    }

    @Test
    void createRejectsOversize() {
        assertThrows(ApiException.class, () -> service.create(me,
                new MediaService.CreateCommand("VIDEO", "video/mp4", 60_000_000L, null, null)));
    }

    @Test
    void backendControlsObjectKey() {
        when(assets.save(any())).thenAnswer(i -> i.getArgument(0));
        var v = service.create(me, new MediaService.CreateCommand("IMAGE", "image/png", null,
                "../../etc/passwd", null));
        assertTrue(v.objectKey().startsWith(me + "/"));
        assertFalse(v.objectKey().contains(".."));
    }

    @Test
    void completeRequiresActualObjectInStorage() {
        var a = owned(MediaAssetEntity.Visibility.PRIVATE);
        when(storage.objectExists(a.getObjectKey())).thenReturn(false);
        var e = assertThrows(ApiException.class, () -> service.complete(me, a.getId(), null, null));
        assertEquals(ErrorCode.MEDIA_NOT_READY, e.code());
    }

    @Test
    void completeRunsProcessorAndPersistsVariants() {
        var a = owned(MediaAssetEntity.Visibility.PRIVATE);
        when(storage.objectExists(any())).thenReturn(true);
        var spec = new MediaProcessor.VariantSpec("THUMBNAIL", "k2", 100, 100, "jpeg", 500L);
        when(processor.process(any(), any()))
                .thenReturn(new MediaProcessor.Result(MediaAssetEntity.ProcessingState.COMPLETED, List.of(spec)));

        service.complete(me, a.getId(), "abc", 500L);
        assertEquals(MediaAssetEntity.UploadState.VERIFIED, a.getUploadState());
        assertEquals(MediaAssetEntity.ProcessingState.COMPLETED, a.getProcessingState());
        verify(variants).save(argThat(v -> "THUMBNAIL".equals(v.getVariantType())));
    }

    @Test
    void completeIsIdempotentAfterProcessing() {
        var a = owned(MediaAssetEntity.Visibility.PRIVATE);
        a.markVerified();
        a.setProcessingState(MediaAssetEntity.ProcessingState.COMPLETED);
        var v = service.complete(me, a.getId(), null, null);   // replay → current state, no error
        assertEquals("VERIFIED", v.uploadState());
        verify(processor, never()).process(any(), any());
    }

    @Test
    void cannotCompleteAnotherUsersAsset() {
        var foreign = new MediaAssetEntity(UUID.randomUUID(), "f/k", "x",
                MediaAssetEntity.MediaType.IMAGE, "image/jpeg",
                MediaAssetEntity.Visibility.PRIVATE);
        when(assets.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        var e = assertThrows(ApiException.class, () -> service.complete(me, foreign.getId(), null, null));
        assertEquals(ErrorCode.NOT_FOUND, e.code());
    }

    @Test
    void privateAssetDeliveryIsOwnerOnly() {
        var foreign = new MediaAssetEntity(UUID.randomUUID(), "f/k", "x",
                MediaAssetEntity.MediaType.IMAGE, "image/jpeg",
                MediaAssetEntity.Visibility.PRIVATE);
        foreign.markUploaded(1L, null);
        foreign.markVerified();
        foreign.setProcessingState(MediaAssetEntity.ProcessingState.COMPLETED);
        when(assets.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        assertThrows(ApiException.class, () -> service.deliveryUrl(me, foreign.getId()));
    }

    @Test
    void publicProcessedAssetDelivers() {
        var foreign = new MediaAssetEntity(UUID.randomUUID(), "f/k", "x",
                MediaAssetEntity.MediaType.IMAGE, "image/jpeg",
                MediaAssetEntity.Visibility.PUBLIC);
        foreign.markUploaded(1L, null);
        foreign.markVerified();
        foreign.setProcessingState(MediaAssetEntity.ProcessingState.COMPLETED);
        when(assets.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        when(storage.createDeliveryUrl(any(), any()))
                .thenReturn(new ObjectStorageClient.PresignedUrl("local://x", "GET", java.time.OffsetDateTime.now()));
        var u = service.deliveryUrl(me, foreign.getId());
        assertNotNull(u.url());
    }
}
