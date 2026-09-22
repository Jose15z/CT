package com.culitostracker.repository;

import com.culitostracker.domain.model.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, UUID> {

    boolean existsByUserId(UUID userId);
}
