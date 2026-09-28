package com.culitostracker.api;

import com.culitostracker.api.dto.UserDtos.ChangePasswordRequest;
import com.culitostracker.api.dto.UserDtos.DeleteAccountRequest;
import com.culitostracker.api.dto.UserDtos.UpdateUserRequest;
import com.culitostracker.api.dto.UserDtos.UserResponse;
import com.culitostracker.application.AccountService;
import com.culitostracker.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AccountService accountService;

    public UserController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    /** Everything the user owns, as one JSON download. */
    @GetMapping("/me/export")
    public ResponseEntity<Map<String, Object>> export(Authentication authentication) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"culitostracker-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(accountService.export(CurrentUser.id(authentication)));
    }

    /** Irreversible: cascades through every table the user owns. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication,
                              @Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(CurrentUser.id(authentication), request.password());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.me(CurrentUser.id(authentication));
    }

    @PatchMapping("/me")
    public UserResponse update(Authentication authentication,
                               @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(CurrentUser.id(authentication), request);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(authentication), request);
    }
}
