package com.culitostracker.api;

import com.culitostracker.api.dto.UserDtos.ChangePasswordRequest;
import com.culitostracker.api.dto.UserDtos.UpdateUserRequest;
import com.culitostracker.api.dto.UserDtos.UserResponse;
import com.culitostracker.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
