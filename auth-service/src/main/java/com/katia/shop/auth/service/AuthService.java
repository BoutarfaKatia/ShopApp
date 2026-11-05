package com.katia.shop.auth.service;

import com.katia.shop.auth.dto.LoginRequest;
import com.katia.shop.auth.dto.LoginTokens;
import com.katia.shop.auth.dto.RegisterRequest;
import com.katia.shop.auth.exception.InvalidRefreshTokenException;
import com.katia.shop.auth.security.JwtService;
import com.katia.shop.auth.security.RefreshTokenService;
import com.katia.shop.user.exception.EmailAlreadyExistException;

import com.katia.shop.user.entity.Role;
import com.katia.shop.user.entity.User;
import com.katia.shop.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistException(email);
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        userRepository.save(user);
        log.info("User registered. userId={}", user.getId());
    }

    @Transactional
    public LoginTokens login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalize(request.email()), request.password()));
        User user = (User) authentication.getPrincipal();
        log.info("Login successful. userId={}", user.getId());
        return new LoginTokens(jwtService.generateAccessToken(user), refreshTokenService.issue(user));
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginTokens refresh(String rawRefreshToken) {
        User user = refreshTokenService.consume(rawRefreshToken);
        return new LoginTokens(jwtService.generateAccessToken(user), refreshTokenService.issue(user));
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
