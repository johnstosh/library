/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.dto.GrokJobDto;
import com.muczynski.library.service.GrokJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Poll the status of a background Grok job started by one of the {@code .../start} endpoints.
 */
@RestController
@RequestMapping("/api/grok-jobs")
public class GrokJobController {

    static final String NOT_FOUND_MESSAGE =
            "This Grok request is no longer available (it expired or the server restarted). Please try again.";

    private final GrokJobService grokJobService;

    public GrokJobController(GrokJobService grokJobService) {
        this.grokJobService = grokJobService;
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getJob(@PathVariable String jobId) {
        return grokJobService.get(jobId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", NOT_FOUND_MESSAGE, "message", NOT_FOUND_MESSAGE)));
    }

    /** 202 Accepted with the RUNNING job status and a Location header for polling. */
    static ResponseEntity<GrokJobDto> accepted(GrokJobDto job) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header("Location", "/api/grok-jobs/" + job.getJobId())
                .body(job);
    }
}
