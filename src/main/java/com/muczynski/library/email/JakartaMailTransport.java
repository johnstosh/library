/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Component;

@Component
public class JakartaMailTransport implements MailTransport {
    @Override
    public void send(MimeMessage message) throws MessagingException {
        Transport.send(message);
    }
}
