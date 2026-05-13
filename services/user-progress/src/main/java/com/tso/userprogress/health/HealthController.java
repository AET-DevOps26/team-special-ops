package com.tso.userprogress.health;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController {

    @GetMapping("/health")
    public ResponseEntity<String> userProgressHealth() {
       return ResponseEntity.ok("RUNNING");
    }
}
