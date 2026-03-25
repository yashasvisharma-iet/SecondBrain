package com.example.demo.repository;

import com.example.demo.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByAuthProviderAndProviderUserId(String authProvider, String providerUserId);

    Optional<AppUser> findByEmail(String email);
}
