package com.career.agent.controller;

import com.career.agent.model.JobMatch;
import com.career.agent.repo.JobMatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobMatchRepository repository;

    @Value("${rapid.api.key}")
    private String rapidApiKey;

    public JobController(JobMatchRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/saved")
    public ResponseEntity<?> getSavedJobs(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(repository.findByUserEmail(principal.getName()));
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveJob(@RequestBody JobMatch job, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        job.setUserEmail(principal.getName());
        return ResponseEntity.ok(repository.save(job));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).build();
        repository.findById(id).ifPresent(job -> {
            if (job.getUserEmail().equals(principal.getName())) {
                repository.delete(job);
            }
        });
        return ResponseEntity.ok().build();
    }
}
