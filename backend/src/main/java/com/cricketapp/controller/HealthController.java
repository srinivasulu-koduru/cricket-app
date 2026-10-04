package com.cricketapp.controller;

import com.cricketapp.dto.HealthResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> checkHealth() {
        HealthResponseDto response = new HealthResponseDto(
                "OK",
                "Cricket App backend is running"
        );
        return ResponseEntity.ok(response);
    }
}
