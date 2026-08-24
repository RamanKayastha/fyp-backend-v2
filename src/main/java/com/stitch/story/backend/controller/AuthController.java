package com.stitch.story.backend.controller;

import com.stitch.story.backend.dtos.*;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.mapper.UserMapper;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse>
    verify(
            @RequestBody
            VerifyDTO request
    ) {

        return ResponseEntity.ok(
                authService.verify(request)
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<String>
    resendOtp(
            @RequestBody
            ResendDTO request
    ) {

        return ResponseEntity.ok(
                authService.resendOtp(request)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request
    ) {

        return ResponseEntity.ok(
                authService.register(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow();

        return ResponseEntity.ok(
                UserMapper.toDTO(user)
        );
    }
}
