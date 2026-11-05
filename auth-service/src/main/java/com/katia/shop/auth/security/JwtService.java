package com.katia.shop.auth.security;

import com.katia.shop.user.entity.User;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final JwtProperties props;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessExpiration()))
                .claim("email", user.getEmail())
                .claim("authorities", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(rsaKey.getKeyID())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}