package com.cricketapp.util;

import com.cricketapp.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TeamIdGenerator {

    private static final String PREFIX = "TEAM";
    private static final int DIGIT_COUNT = 6;
    private static final int MAX_RETRIES = 10;
    private final SecureRandom random = new SecureRandom();
    private final TeamRepository teamRepository;

    public TeamIdGenerator(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Generates a unique permanent Team ID in the format TEAMXXXXXX (TEAM + 6 digits).
     * Retries automatically if a collision occurs.
     */
    public String generateUniqueTeamId() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            int number = random.nextInt(1_000_000); // 0 to 999999
            String candidateId = String.format("%s%06d", PREFIX, number);

            if (!teamRepository.existsByTeamId(candidateId)) {
                return candidateId;
            }
        }
        throw new IllegalStateException("Failed to generate a unique Team ID after maximum retries");
    }
}
