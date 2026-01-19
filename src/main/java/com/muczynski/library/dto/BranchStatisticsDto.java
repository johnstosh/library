/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BranchStatisticsDto {
    private Long branchId;
    private String branchName;
    private Long bookCount;
    private Long activeLoansCount;
}
