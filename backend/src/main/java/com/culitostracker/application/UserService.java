package com.culitostracker.application;

import com.culitostracker.api.dto.UserDtos.ChangePasswordRequest;
import com.culitostracker.api.dto.UserDtos.UpdateUserRequest;
import com.culitostracker.api.dto.UserDtos.UserResponse;
import com.culitostracker.domain.model.User;
import com.culitostracker.domain.service.DomainRuleException;
import com.culitostracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AvatarService avatarService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return UserResponse.from(requireUser(userId), avatarService.exists(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.preferredLanguage() != null) {
            String lang = request.preferredLanguage().toLowerCase(Locale.ROOT);
            user.setPreferredLanguage(lang.startsWith("en") ? "en" : "es");
        }
        if (request.avatarEmoji() != null) {
            user.setAvatarEmoji(request.avatarEmoji().isBlank() ? null : request.avatarEmoji());
        }
        if (request.relationshipSituation() != null) {
            user.setRelationshipSituation(request.relationshipSituation());
        }
        if (request.timezone() != null) {
            user.setTimezone(validZone(request.timezone()));
        }
        if (request.remindersEnabled() != null) {
            user.setRemindersEnabled(request.remindersEnabled());
        }
        return UserResponse.from(userRepository.save(user), avatarService.exists(userId));
    }

    /** The browser reports its zone when subscribing to reminders. */
    @Transactional
    public void updateTimezone(UUID userId, String timezone) {
        User user = requireUser(userId);
        user.setTimezone(validZone(timezone));
        userRepository.save(user);
    }

    private static String validZone(String zone) {
        try {
            return java.time.ZoneId.of(zone).getId();
        } catch (RuntimeException e) {
            throw new DomainRuleException("user.invalidTimezone", "Unknown time zone");
        }
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new DomainRuleException("auth.invalidCredentials", "Current password does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
