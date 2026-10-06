/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status of a background Grok job (long AI actions run off the request thread so the
 * browser never waits minutes on one HTTP request).
 *
 * <ul>
 *   <li>{@code RUNNING} - still working; poll again.</li>
 *   <li>{@code SUCCEEDED} - {@code result} holds exactly what the synchronous endpoint returns.</li>
 *   <li>{@code FAILED} - {@code error} holds the user-facing message and {@code httpStatus}
 *       the status the synchronous endpoint would have returned (e.g. 402 out of credits,
 *       400 no photos).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrokJobDto {
    private String jobId;
    private String kind;
    private String status;
    private Object result;
    private String error;
    private Integer httpStatus;
}
