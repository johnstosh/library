/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.dto;

import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.domain.MailingListTopic;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MailingListSignupRequest {
    private String email;
    private List<MailingListTopic> lists;
    private MailingFrequency frequency;
}
