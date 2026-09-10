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
public class FavoriteListMembershipDto {
    private String listName;
    private List<Long> bookIds = new ArrayList<>();
    private List<Long> authorIds = new ArrayList<>();
}
