package com.katia.shop.auth.security;

import com.katia.shop.auth.repository.RefreshTokenRepository;
import com.katia.shop.auth.entity.RefreshToken;
import com.katia.shop.auth.exception.InvalidRefreshTokenException;
import com.katia.shop.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(props.refreshExpiration()));
        repository.save(entity);
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public User consume(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken stored = repository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked()) {
            repository.revokeAllByUserId(stored.getUser().getId());
            log.warn("Refresh token reuse detected. userId={}", stored.getUser().getId());
            throw new InvalidRefreshTokenException();
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }
        if (repository.revokeIfActive(hash) == 0) {
            throw new InvalidRefreshTokenException();
        }
        return stored.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.revokeIfActive(hash(rawToken));
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}