package com.cricketapp.util;

import com.cricketapp.repository.MatchRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class MatchIdGenerator {

    private static final String PREFIX = "MATCH";
    private static final int MAX_RETRIES = 10;
    private final SecureRandom random = new SecureRandom();
    private final MatchRepository matchRepository;

    public MatchIdGenerator(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /**
     * Generates a unique permanent Match ID in the format MATCHXXXXXX (MATCH + 6 digits).
     * Retries automatically if a collision occurs.
     */
    public String generateUniqueMatchId() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            int number = 100000 + random.nextInt(900000); // 100000 to 999999
            String candidateId = String.format("%s%d", PREFIX, number);

            if (!matchRepository.existsByMatchId(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("Failed to generate a unique Match ID after maximum retries");
    }
}
