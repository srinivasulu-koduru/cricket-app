package com.cricketapp.util;

import com.cricketapp.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class UserIdGenerator {

    private static final String PREFIX = "CRK";
    private static final int DIGIT_COUNT = 6;
    private static final int MAX_RETRIES = 10;
    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepository;

    public UserIdGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Generates a unique Cricket User ID in the format CRKXXXXXX (CRK + 6 digits).
     * Retries automatically if a collision occurs.
     */
    public String generateUniqueUserId() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            int number = random.nextInt(1_000_000); // 0 to 999999
            String candidateId = String.format("%s%06d", PREFIX, number);

            if (!userRepository.existsByUserId(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("Failed to generate a unique User ID after maximum retries");
    }
}
