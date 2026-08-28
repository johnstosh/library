/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transport-neutral email payload. Senders turn this into SMTP, SendGrid JSON,
 * a webhook body, or a log line.
 */
public class EmailMessage {

    private final List<String> to = new ArrayList<>();
    private String fromAddress;
    private String fromName;
    private String subject;
    private String textBody;
    private String htmlBody;
    private String event;
    private final Map<String, Object> eventPayload = new LinkedHashMap<>();

    public List<String> getTo() {
        return to;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Map<String, Object> getEventPayload() {
        return eventPayload;
    }
}
