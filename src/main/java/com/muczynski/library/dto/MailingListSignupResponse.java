/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.domain.MailingListTopic;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MailingListSignupResponse {

    public static final String CREATED = "created";
    public static final String RESUBSCRIBED = "resubscribed";
    public static final String ALREADY_SUBSCRIBED = "already_subscribed";

    private List<Line> results = new ArrayList<>();

    @Getter
    @Setter
    public static class Line {
        private MailingListTopic list;
        private String displayName;
        private MailingFrequency frequency;
        private String status;
        private String message;
        private boolean sent;
    }
}
