package com.katia.shop.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String ISSUER = "shop-api";
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS = "access";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public String generateAccessToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim(TYPE_CLAIM, ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessExpiration())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String validateAndGetUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .require(TYPE_CLAIM, ACCESS)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String generateExpiredAccessToken(UserDetails user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim(TYPE_CLAIM, ACCESS)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}





