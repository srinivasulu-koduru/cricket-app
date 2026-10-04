package com.cricketapp.controller;

import com.cricketapp.service.DatabaseSeederService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final DatabaseSeederService databaseSeederService;

    public DevController(DatabaseSeederService databaseSeederService) {
        this.databaseSeederService = databaseSeederService;
    }

    @PostMapping("/reset-and-seed")
    public ResponseEntity<Map<String, Object>> resetAndSeed() {
        databaseSeederService.resetAndSeedDatabase();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Database successfully reset and seeded with 28 player profiles, 3 teams, fixtures, assigned scorers, and Playing XIs."
        ));
    }
}
