package com.restaurantos.authenticationservice.repository;

import com.restaurantos.authenticationservice.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findByUsernameAndCodeAndUsedFalse(String username, String code);

}
