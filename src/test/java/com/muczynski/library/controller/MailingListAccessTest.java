/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.controller;

import com.muczynski.library.domain.MailingListTopic;
import com.muczynski.library.repository.MailingListSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Patrons open the signup page and unsubscribe without logging in.
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

    @Test
    void mailingListsPageIsPublic() throws Exception {
        mockMvc.perform(get("/mailing-lists"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
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
    void signupKeepsTheFirstFrequencyAndUnsubscribeIsIdempotent() throws Exception {
        String email = "mailing-" + UUID.randomUUID() + "@example.com";
        String created = """
                {"email":"%s","lists":["ADULT_FICTION"],"frequency":"MONTHLY"}
                """.formatted(email);
        mockMvc.perform(post("/api/public/mailing-lists/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(created))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("created"))
                .andExpect(jsonPath("$.results[0].list").value("ADULT_FICTION"))
                .andExpect(jsonPath("$.results[0].frequency").value("MONTHLY"));

        String again = """
                {"email":"%s","lists":["ADULT_FICTION"],"frequency":"DAILY"}
                """.formatted(email);
        mockMvc.perform(post("/api/public/mailing-lists/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(again))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("already_subscribed"))
                .andExpect(jsonPath("$.results[0].frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.results[0].message",
                        containsString("Unsubscribe first to change frequency")));

        var row = subscriptions.findByEmailAndTopic(email, MailingListTopic.ADULT_FICTION).orElseThrow();
        assertEquals(com.muczynski.library.domain.MailingFrequency.MONTHLY, row.getFrequency());
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
}
