package com.culitostracker.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/** A user's profile photo, already validated, cropped square and resized. */
@Entity
@Table(name = "user_avatars")
public class UserAvatar {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private byte[] image;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Instant getUpdatedAt() { return updatedAt; }
}
