package com.katia.shop.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Stream;

@Entity
@Getter
@Setter
@NoArgsConstructor

public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Email
    @Column( nullable = false , unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(
                Stream.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                role.getPermissions().stream()
                        .map(permission ->
                                new SimpleGrantedAuthority(permission.name()
                                ))
        ).toList();


    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

