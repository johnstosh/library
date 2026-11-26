/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.domain.MailingListTopic;
import com.muczynski.library.domain.User;
import com.muczynski.library.repository.MailingListSubscriptionRepository;
import com.muczynski.library.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Signup requires a logged-in account. Unsubscribe stays public.
 * Outbound mail stays on the test default, which does not send.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailingListAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MailingListSubscriptionRepository subscriptions;

    @Autowired
    private UserRepository users;

    @Test
    void mailingListsPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/mailing-lists"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMailingListsPageIsTheSpa() throws Exception {
        User patron = savePatron("page", "page@example.com", "local", null);
        mockMvc.perform(get("/mailing-lists").with(asUser(patron)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void anonymousSubscribeIsRejectedAndTheOldPublicPathIsGone() throws Exception {
        String body = """
                {"email":"anon@example.com","lists":["ADULT_FICTION"],"frequency":"MONTHLY"}
                """;
        mockMvc.perform(post("/api/mailing-lists/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        int oldPath = mockMvc.perform(post("/api/public/mailing-lists/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
        assertNotEquals(200, oldPath);
        assertTrue(subscriptions.findByEmailAndTopic("anon@example.com", MailingListTopic.ADULT_FICTION).isEmpty());
    }

    @Test
    void unknownUnsubscribeLinkIsNotALoginWall() throws Exception {
        mockMvc.perform(get("/mailing-lists/unsubscribe/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This unsubscribe link is not valid.")));
        mockMvc.perform(post("/mailing-lists/unsubscribe/not-a-real-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("This unsubscribe link is not valid.")));
    }

    @Test
    void signupUsesTheAccountEmailAndIgnoresTheBody() throws Exception {
        String account = "account-" + UUID.randomUUID() + "@example.com";
        String ignored = "ignored-" + UUID.randomUUID() + "@example.com";
        User patron = savePatron("signup", account, "local", null);
        String created = """
                {"email":"%s","lists":["ADULT_FICTION"],"frequency":"MONTHLY"}
                """.formatted(ignored);
        mockMvc.perform(post("/api/mailing-lists/subscriptions")
                        .with(asUser(patron))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(created))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("created"))
                .andExpect(jsonPath("$.results[0].list").value("ADULT_FICTION"))
                .andExpect(jsonPath("$.results[0].frequency").value("MONTHLY"));

        assertTrue(subscriptions.findByEmailAndTopic(ignored, MailingListTopic.ADULT_FICTION).isEmpty());
        var row = subscriptions.findByEmailAndTopic(account, MailingListTopic.ADULT_FICTION).orElseThrow();
        assertEquals(MailingFrequency.MONTHLY, row.getFrequency());

        String again = """
                {"email":"%s","lists":["ADULT_FICTION"],"frequency":"DAILY"}
                """.formatted(ignored);
        mockMvc.perform(post("/api/mailing-lists/subscriptions")
                        .with(asUser(patron))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(again))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("already_subscribed"))
                .andExpect(jsonPath("$.results[0].frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.results[0].message",
                        containsString("Unsubscribe first to change frequency")));

        row = subscriptions.findByEmailAndTopic(account, MailingListTopic.ADULT_FICTION).orElseThrow();
        assertEquals(MailingFrequency.MONTHLY, row.getFrequency());
        String token = row.getUnsubscribeToken();
        assertTrue(token != null && token.length() >= 32);

        mockMvc.perform(get("/mailing-lists/unsubscribe/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have been unsubscribed from Adult Fiction.")));
        mockMvc.perform(get("/mailing-lists/unsubscribe/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You are already unsubscribed from Adult Fiction.")));
        mockMvc.perform(post("/mailing-lists/unsubscribe/" + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You are already unsubscribed from Adult Fiction.")));
    }

    @Test
    void signupWithoutAnAccountEmailIsRejected() throws Exception {
        User local = savePatron("noemail", null, "local", null);
        User sso = savePatron("sso-noemail", null, "google", "subject-no-email");
        String body = """
                {"email":"typed@example.com","lists":["ADULT_FICTION"],"frequency":"WEEKLY"}
                """;
        mockMvc.perform(post("/api/mailing-lists/subscriptions")
                        .with(asUser(local))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Save an email address")));
        mockMvc.perform(post("/api/mailing-lists/subscriptions")
                        .with(asUser(sso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be added")));
        assertTrue(subscriptions.findByEmailAndTopic("typed@example.com", MailingListTopic.ADULT_FICTION).isEmpty());
    }

    @Test
    void changingTheAccountEmailMovesExistingSubscriptions() throws Exception {
        String oldEmail = "old-" + UUID.randomUUID() + "@example.com";
        String newEmail = "new-" + UUID.randomUUID() + "@example.com";
        User patron = savePatron("mover", oldEmail, "local", null);
        mockMvc.perform(post("/api/mailing-lists/subscriptions")
                        .with(asUser(patron))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lists":["READ_ALOUD_BOOKS"],"frequency":"WEEKLY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("created"));
        String token = subscriptions.findByEmailAndTopic(oldEmail, MailingListTopic.READ_ALOUD_BOOKS)
                .orElseThrow()
                .getUnsubscribeToken();

        mockMvc.perform(put("/api/user-settings")
                        .with(asUser(patron))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(newEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        assertTrue(subscriptions.findByEmailAndTopic(oldEmail, MailingListTopic.READ_ALOUD_BOOKS).isEmpty());
        var moved = subscriptions.findByEmailAndTopic(newEmail, MailingListTopic.READ_ALOUD_BOOKS).orElseThrow();
        assertEquals(token, moved.getUnsubscribeToken());
        assertTrue(moved.isActive());
        assertEquals(MailingFrequency.WEEKLY, moved.getFrequency());
    }

    private User savePatron(String name, String email, String provider, String subject) {
        User user = new User();
        user.setUsername(name + "-" + UUID.randomUUID());
        user.setUserIdentifier(UUID.randomUUID().toString());
        user.setPassword("password");
        user.setSsoProvider(provider);
        user.setSsoSubjectId(subject);
        user.setEmail(email);
        return users.save(user);
    }

    private static RequestPostProcessor asUser(User user) {
        return user(String.valueOf(user.getId())).authorities(new SimpleGrantedAuthority("USER"));
    }
}
