/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.FavoriteItemType;
import com.muczynski.library.dto.FavoriteItemDto;
import com.muczynski.library.dto.FavoriteSummaryDto;
import com.muczynski.library.dto.FavoriteUpdateDto;
import com.muczynski.library.service.FavoriteService;
import com.muczynski.library.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/summary")
    public ResponseEntity<FavoriteSummaryDto> getSummary(Principal principal) {
        return ResponseEntity.ok(favoriteService.getSummary(userId(principal)));
    }

    @GetMapping("/item")
    public ResponseEntity<FavoriteItemDto> getItem(
            Principal principal,
            Authentication authentication,
            @RequestParam FavoriteItemType itemType,
            @RequestParam Long itemId) {
        return ResponseEntity.ok(favoriteService.getItem(
                userId(principal), itemType, itemId, SecurityUtils.isLibrarian(authentication)));
    }

    @PutMapping("/item")
    public ResponseEntity<FavoriteItemDto> replaceItem(
            Principal principal,
            Authentication authentication,
            @RequestBody FavoriteUpdateDto update) {
        return ResponseEntity.ok(favoriteService.replaceItem(
                userId(principal), update, SecurityUtils.isLibrarian(authentication)));
    }

    private Long userId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return Long.parseLong(principal.getName());
    }
}
