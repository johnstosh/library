package com.muczynski.library.controller;

import com.muczynski.library.domain.Applied;
import com.muczynski.library.service.AppliedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/apply")
public class AppliedController {

    @Autowired
    private AppliedService appliedService;

    @GetMapping("/apply-for-card.html")
    public String applyForCard(Model model) {
        model.addAttribute("applied", new Applied());
        return "apply-for-card";
    }

    @PostMapping
    public String applyForCard(@ModelAttribute Applied applied) {
        appliedService.createApplied(applied);
        return "redirect:/";
    }

    @GetMapping("/api")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @ResponseBody
    public ResponseEntity<List<Applied>> getAllApplied() {
        List<Applied> applied = appliedService.getAllApplied();
        return ResponseEntity.ok(applied);
    }

    @PostMapping("/api")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @ResponseBody
    public ResponseEntity<Applied> createApplied(@RequestBody Applied applied) {
        Applied createdApplied = appliedService.createApplied(applied);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdApplied);
    }

    @PutMapping("/api/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @ResponseBody
    public ResponseEntity<Applied> updateApplied(@PathVariable Long id, @RequestBody Applied applied) {
        Applied updatedApplied = appliedService.updateApplied(id, applied);
        return ResponseEntity.ok(updatedApplied);
    }

    @DeleteMapping("/api/{id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @ResponseBody
    public ResponseEntity<Void> deleteApplied(@PathVariable Long id) {
        appliedService.deleteApplied(id);
        return ResponseEntity.noContent().build();
    }
}