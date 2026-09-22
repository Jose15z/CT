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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return UserResponse.from(requireUser(userId));
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
        return UserResponse.from(userRepository.save(user));
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
