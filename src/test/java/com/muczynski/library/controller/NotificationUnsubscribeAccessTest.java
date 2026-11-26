/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.UserRepository;
import com.muczynski.library.service.NotificationUnsubscribeService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The notice unsubscribe link is public and only turns off receiveEmails.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationUnsubscribeAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private NotificationUnsubscribeService unsubscribes;

    @Test
    void unknownTokenIsNotALoginWall() throws Exception {
        mockMvc.perform(get("/notifications/unsubscribe/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("This unsubscribe link is not valid.")));
        mockMvc.perform(post("/notifications/unsubscribe/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("This unsubscribe link is not valid.")));
    }

    @Test
    void linkTurnsOffEmailNoticesAndCanBeRepeated() throws Exception {
        String email = "notices-" + UUID.randomUUID() + "@example.com";
        User patron = savePatron(email);
        assertTrue(patron.isReceiveEmails());
        assertNull(patron.getNotificationUnsubscribeToken());

        String first = unsubscribes.urlForAddress(email.toUpperCase());
        String again = unsubscribes.urlForAddress("  " + email + "  ");
        assertNotNull(first);
        assertEquals(first, again);
        String token = users.findById(patron.getId()).orElseThrow().getNotificationUnsubscribeToken();
        assertNotNull(token);
        assertEquals(64, token.length());
        assertTrue(first.endsWith("/notifications/unsubscribe/" + token));
        assertNull(unsubscribes.urlForAddress("nobody-" + UUID.randomUUID() + "@example.com"));

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationUnsubscribeService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String html = mockMvc.perform(get("/notifications/unsubscribe/" + token))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                    .andExpect(content().string(containsString("You will no longer receive email notifications.")))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertFalse(html.contains(token));
            assertFalse(users.findById(patron.getId()).orElseThrow().isReceiveEmails());

            mockMvc.perform(get("/notifications/unsubscribe/" + token))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("You are already unsubscribed from email notifications.")));
            String plain = mockMvc.perform(post("/notifications/unsubscribe/" + token))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(containsString("You are already unsubscribed from email notifications.")))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertFalse(plain.contains(token));
            assertFalse(users.findById(patron.getId()).orElseThrow().isReceiveEmails());
            assertEquals(token, users.findById(patron.getId()).orElseThrow().getNotificationUnsubscribeToken());

            String logged = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertFalse(logged.contains(token));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private User savePatron(String email) {
        User user = new User();
        user.setUsername("notices-" + UUID.randomUUID());
        user.setUserIdentifier(UUID.randomUUID().toString());
        user.setPassword("password");
        user.setEmail(email);
        user.setReceiveEmails(true);
        return users.save(user);
    }
}
