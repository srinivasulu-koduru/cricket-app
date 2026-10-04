package com.cricketapp.repository;

import com.cricketapp.entity.OtpPurpose;
import com.cricketapp.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    List<OtpVerification> findByEmailAndPurpose(String email, OtpPurpose purpose);

    void deleteByEmail(String email);
}
