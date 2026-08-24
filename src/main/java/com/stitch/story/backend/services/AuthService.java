package com.stitch.story.backend.services;


import com.stitch.story.backend.dtos.*;
import com.stitch.story.backend.entities.PendingRegistration;
import com.stitch.story.backend.entities.User;
import com.stitch.story.backend.entities.enums.AuthProvider;
import com.stitch.story.backend.entities.enums.Role;
import com.stitch.story.backend.exceptions.BadRequestException;
import com.stitch.story.backend.mapper.UserMapper;
import com.stitch.story.backend.repositories.PendingRegistrationRepository;
import com.stitch.story.backend.repositories.UserRepository;
import com.stitch.story.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PendingRegistrationRepository pendingRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    // Registration
    public String register(RegisterRequest request) {

        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException(
                    "Email already exists."
            );
        }

        pendingRepository.deleteByEmail(request.getEmail());

        String otp = otpService.generateOTP();

        PendingRegistration pending =
                PendingRegistration.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role(Role.USER)
                        .authProvider(AuthProvider.LOCAL)
                        .otp(otp)
                        .expiryTime(LocalDateTime.now().plusMinutes(5))
                        .attempts(0)
                        .resendAvailableAt(
                                LocalDateTime.now()
                                        .plusSeconds(60)
                        )
                        .build();

        pendingRepository.save(pending);

        emailService.sendOtp(
                request.getEmail(),
                otp
        );

        return "OTP sent successfully.";
    }

    //verify

    public AuthResponse verify(VerifyDTO request) {

        PendingRegistration pending = pendingRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("OTP not found."));

        if (pending.getExpiryTime()
                .isBefore(
                        LocalDateTime.now()
                )) {
            throw new BadRequestException(
                    "OTP expired."
            );
        }

        if (pending.getAttempts() >= 5) {

            pendingRepository.delete(
                    pending
            );

            throw new BadRequestException(
                    "Maximum verification attempts exceeded. Please register again."
            );
        }

        if (!pending.getOtp()
                .equals(request.getOtp())) {

            pending.setAttempts(
                    pending.getAttempts() + 1
            );

            pendingRepository.save(
                    pending
            );

            throw new BadRequestException(
                    "Invalid OTP. Remaining attempts: "
                            + (5 - pending.getAttempts())
            );
        }

        User user =
                User.builder()
                        .username(pending.getUsername())
                        .email(pending.getEmail())
                        .password(pending.getPassword())
                        .role(pending.getRole())
                        .authProvider(AuthProvider.LOCAL)
                        .build();

        repository.save(user);

        pendingRepository.delete(pending);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userDTO(UserMapper.toDTO(user))
                .build();
    }

    public String resendOtp(ResendDTO request) {

        PendingRegistration pending =
                pendingRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Registration not found."
                                )
                        );

        if (pending.getResendAvailableAt()
                .isAfter(LocalDateTime.now())) {

            long secondsRemaining =
                    Duration.between(
                            LocalDateTime.now(),
                            pending.getResendAvailableAt()
                    ).getSeconds();

            throw new BadRequestException(
                    "Please wait "
                            + secondsRemaining
                            + " seconds before requesting another OTP."
            );
        }

        String otp =
                otpService.generateOTP();

        pending.setOtp(otp);

        pending.setAttempts(0);

        pending.setExpiryTime(
                LocalDateTime.now()
                        .plusMinutes(5)
        );

        pending.setResendAvailableAt(
                LocalDateTime.now()
                        .plusSeconds(60)
        );

        pendingRepository.save(pending);

        emailService.sendOtp(
                pending.getEmail(),
                otp
        );

        return "OTP resent successfully.";
    }


    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User with given email not found:" + request.getEmail()));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userDTO(UserMapper.toDTO(user))
                .build();
    }
}