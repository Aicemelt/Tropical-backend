package com.tropical.backend.auth.repository;

import com.tropical.backend.auth.entity.TermsAndPolicies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsAndPoliciesRepository extends JpaRepository<TermsAndPolicies,Long> {

}
