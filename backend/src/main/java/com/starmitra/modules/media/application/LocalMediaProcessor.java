package com.starmitra.modules.media.application;

import com.starmitra.modules.media.persistence.MediaAssetEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

/**
 * Local processor — real content validation where the JDK supports it:
 *  - IMAGE: ImageIO decodes actual bytes (spoofed MIME rejected) + emits a
 *    THUMBNAIL variant record (bytes copied; no real resize lib — documented).
 *  - VIDEO/AUDIO/DOCUMENT: NOT_REQUIRED (no transcoding platform for MVP).
 * ImageIO decode also strips reliance on extension/MIME claims.
 */
@Component
@Profile({"local", "test", "uat", "default"})
public class LocalMediaProcessor implements MediaProcessor {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaProcessor.class);

    @Override
    public Result process(MediaAssetEntity asset, ObjectStorageClient storage) {
        if (asset.getMediaType() != MediaAssetEntity.MediaType.IMAGE) {
            return new Result(MediaAssetEntity.ProcessingState.NOT_REQUIRED, List.of());
        }
        var bytes = storage.readObject(asset.getObjectKey());
        if (bytes.isEmpty()) {
            return new Result(MediaAssetEntity.ProcessingState.FAILED, List.of());
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes.get()));
            if (img == null) {
                // declared image/* but bytes aren't a decodable image — spoof/corrupt
                return new Result(MediaAssetEntity.ProcessingState.FAILED, List.of());
            }
            String thumbKey = asset.getOwnerUserId() + "/" + UUID.randomUUID() + ".thumb";
            storage.writeObject(thumbKey, bytes.get(), asset.getMimeType());   // placeholder derivative
            var spec = new VariantSpec("THUMBNAIL", thumbKey,
                    img.getWidth(), img.getHeight(),
                    asset.getMimeType().split("/")[1], (long) bytes.get().length);
            return new Result(MediaAssetEntity.ProcessingState.COMPLETED, List.of(spec));
        } catch (Exception e) {
            log.warn("image processing failed for asset {}", asset.getId());
            return new Result(MediaAssetEntity.ProcessingState.FAILED, List.of());
        }
    }
}
