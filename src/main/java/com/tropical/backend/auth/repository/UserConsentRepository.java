package com.tropical.backend.auth.repository;

import com.tropical.backend.auth.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
}
