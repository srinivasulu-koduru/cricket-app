package com.cricketapp.runner;

import com.cricketapp.service.DatabaseSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.db.seed-on-startup", havingValue = "true", matchIfMissing = false)
public class DatabaseSeederRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeederRunner.class);

    private final DatabaseSeederService databaseSeederService;

    public DatabaseSeederRunner(DatabaseSeederService databaseSeederService) {
        this.databaseSeederService = databaseSeederService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Executing database reset and seeding...");
        try {
            databaseSeederService.resetAndSeedDatabase();
            logger.info("Database seeding completed successfully!");
        } catch (Exception e) {
            logger.error("Failed to seed database", e);
        }
    }
}
