package com.katia.shop.auth.security;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
public class JwtKeyConfig {

    @Bean
    RSAKey rsaKey(JwtProperties props) throws GeneralSecurityException, JOSEException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privateKey;

        if (StringUtils.hasText(props.privateKey())) {
            byte[] der = Base64.getDecoder().decode(props.privateKey().trim());
            privateKey = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } else if (props.allowEphemeralKey()) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            privateKey = (RSAPrivateCrtKey) generator.generateKeyPair().getPrivate();
            log.warn("No jwt.private-key configured: using an EPHEMERAL signing key (development only)");
        } else {
            throw new IllegalStateException(
                    "jwt.private-key is required (or set jwt.allow-ephemeral-key=true for development)");
        }
        if (privateKey.getModulus().bitLength() < 2048) {
            throw new IllegalStateException("RSA key must be at least 2048 bits");
        }

        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyIDFromThumbprint()
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }
}