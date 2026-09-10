/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteSummaryDto {
    private List<Long> favoriteBookIds = new ArrayList<>();
    private List<Long> favoriteAuthorIds = new ArrayList<>();
    private List<FavoriteListMembershipDto> lists = new ArrayList<>();
}
