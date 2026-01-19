/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchDto {
    private Long id;
    private String branchName;
    private String librarySystemName;

    /**
     * Backward compatibility: old JSON exports used "name" field.
     * Maps to branchName for import compatibility.
     */
    @JsonSetter("name")
    public void setName(String name) {
        if (this.branchName == null) {
            this.branchName = name;
        }
    }
}
