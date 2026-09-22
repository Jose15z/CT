package com.culitostracker.application;

import com.culitostracker.domain.model.UserAvatar;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.UserAvatarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Profile photos. Uploads are validated by magic bytes (never by filename),
 * center-cropped to a square and resized to 256px JPEG, which keeps every
 * avatar around 10-30KB — small enough to live in Postgres on free tiers.
 * A photo is only ever served to its owner.
 */
@Service
public class AvatarService {

    static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    static final int TARGET_SIZE = 256;

    private final UserAvatarRepository avatarRepository;

    public AvatarService(UserAvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    @Transactional
    public void store(UUID userId, byte[] upload) {
        if (upload == null || upload.length == 0) {
            throw new DomainRuleException("avatar.invalidImage", "Empty upload");
        }
        if (upload.length > MAX_UPLOAD_BYTES) {
            throw new DomainRuleException("avatar.tooLarge", "Avatar exceeds 2MB");
        }
        if (!looksLikeSupportedImage(upload)) {
            throw new DomainRuleException("avatar.invalidImage", "Only JPEG or PNG images are accepted");
        }

        byte[] processed = cropAndResize(upload);

        UserAvatar avatar = avatarRepository.findById(userId).orElseGet(() -> {
            UserAvatar a = new UserAvatar();
            a.setUserId(userId);
            return a;
        });
        avatar.setImage(processed);
        avatar.setContentType("image/jpeg");
        avatarRepository.save(avatar);
    }

    @Transactional(readOnly = true)
    public Optional<UserAvatar> get(UUID userId) {
        return avatarRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID userId) {
        return avatarRepository.existsByUserId(userId);
    }

    @Transactional
    public void delete(UUID userId) {
        avatarRepository.deleteById(userId);
    }

    /** JPEG (FF D8) or PNG (89 'PNG') by magic bytes, not by extension. */
    private boolean looksLikeSupportedImage(byte[] bytes) {
        if (bytes.length < 4) {
            return false;
        }
        boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
        boolean png = (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
        return jpeg || png;
    }

    private byte[] cropAndResize(byte[] upload) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(upload));
        } catch (IOException e) {
            source = null;
        }
        if (source == null || source.getWidth() < 8 || source.getHeight() < 8) {
            throw new DomainRuleException("avatar.invalidImage", "Unreadable image");
        }

        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        int target = Math.min(TARGET_SIZE, side);

        // Re-encoding to RGB JPEG also strips EXIF metadata (GPS included).
        BufferedImage out = new BufferedImage(target, target, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, target, target);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source.getSubimage(x, y, side, side), 0, 0, target, target, null);
        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new DomainRuleException("avatar.invalidImage", "Could not process image");
        }
    }
}
