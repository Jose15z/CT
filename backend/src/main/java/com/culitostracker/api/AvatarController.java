package com.culitostracker.api;

import com.culitostracker.application.AvatarService;
import com.culitostracker.application.NotFoundException;
import com.culitostracker.domain.model.UserAvatar;
import com.culitostracker.domain.service.DomainRuleException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/** Profile photo of the authenticated user. Nobody else can fetch it. */
@RestController
@RequestMapping("/api/users/me/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upload(Authentication authentication, @RequestParam("file") MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new DomainRuleException("avatar.invalidImage", "Could not read upload");
        }
        avatarService.store(CurrentUser.id(authentication), bytes);
    }

    @GetMapping
    public ResponseEntity<byte[]> get(Authentication authentication) {
        UserAvatar avatar = avatarService.get(CurrentUser.id(authentication))
                .orElseThrow(() -> new NotFoundException("No avatar"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(avatar.getImage());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication) {
        avatarService.delete(CurrentUser.id(authentication));
    }
}
