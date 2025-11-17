package com.muczynski.library.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LocCallNumberResponse {
    private String callNumber;
    private String source;
    private int matchCount;
    private List<String> allCallNumbers;
}
