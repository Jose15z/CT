package com.culitostracker.api;

import com.culitostracker.api.dto.InviteDtos.InvitePreview;
import com.culitostracker.api.dto.InviteDtos.InviteResponse;
import com.culitostracker.api.dto.InviteDtos.LinkResponse;
import com.culitostracker.application.InviteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    /** Owner only: mints a one-time link valid for seven days. */
    @PostMapping("/partners/{partnerId}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse create(Authentication authentication, @PathVariable UUID partnerId) {
        return inviteService.create(CurrentUser.id(authentication), partnerId);
    }

    /** Public: the invitee may not have an account yet. */
    @GetMapping("/invites/{token}")
    public InvitePreview preview(@PathVariable String token) {
        return inviteService.preview(token);
    }

    @PostMapping("/invites/{token}/accept")
    public LinkResponse accept(Authentication authentication, @PathVariable String token) {
        return inviteService.accept(CurrentUser.id(authentication), token);
    }

    @GetMapping("/links")
    public List<LinkResponse> myLinks(Authentication authentication) {
        return inviteService.myLinks(CurrentUser.id(authentication));
    }

    @DeleteMapping("/links/{partnerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(Authentication authentication, @PathVariable UUID partnerId) {
        inviteService.unlink(CurrentUser.id(authentication), partnerId);
    }
}
