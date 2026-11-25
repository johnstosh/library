/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.muczynski.library.domain.Book;
import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.domain.MailingListSubscription;
import com.muczynski.library.domain.MailingListTopic;
import com.muczynski.library.dto.MailingListSignupResponse;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.OutgoingEmail;
import com.muczynski.library.repository.BranchRepository;
import com.muczynski.library.repository.MailingListSubscriptionRepository;
import com.muczynski.library.service.MailingListWindows.InstantWindow;
import com.muczynski.library.service.MailingListWindows.SendOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailingListServiceTest {

    @Mock
    private MailingListSubscriptionRepository subscriptions;

    @Mock
    private MailingListCatalog catalog;

    @Mock
    private GlobalSettingsService globalSettingsService;

    @Mock
    private BranchRepository branchRepository;

    private final List<MailingListSubscription> rows = new ArrayList<>();
    private final FakeSender sender = new FakeSender();
    private MailingListService service;

    @BeforeEach
    void setUp() {
        lenient().when(subscriptions.findByEmailAndTopic(anyString(), any())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            MailingListTopic topic = invocation.getArgument(1);
            return rows.stream()
                    .filter(row -> row.getEmail().equals(email) && row.getTopic() == topic)
                    .findFirst();
        });
        lenient().when(subscriptions.save(any(MailingListSubscription.class))).thenAnswer(invocation -> {
            MailingListSubscription subscription = invocation.getArgument(0);
            if (subscription.getId() == null) {
                subscription.setId((long) rows.size() + 1);
                rows.add(subscription);
            } else if (rows.stream().noneMatch(row -> subscription.getId().equals(row.getId()))) {
                rows.add(subscription);
            }
            return subscription;
        });
        lenient().when(subscriptions.findByActiveTrueAndFrequency(any())).thenAnswer(invocation -> {
            MailingFrequency frequency = invocation.getArgument(0);
            return rows.stream().filter(row -> row.isActive() && row.getFrequency() == frequency).toList();
        });
        lenient().when(subscriptions.findByUnsubscribeToken(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            return rows.stream().filter(row -> token.equals(row.getUnsubscribeToken())).findFirst();
        });
        lenient().when(catalog.feastBooks()).thenReturn(List.of());
        lenient().when(catalog.addedBetween(any(), any())).thenReturn(List.of());
        lenient().when(catalog.tagged(any())).thenReturn(List.of());
        lenient().when(branchRepository.findAll()).thenReturn(List.of());

        GlobalSettings settings = new GlobalSettings();
        settings.setEmailMethod(EmailMethod.LOG);
        settings.setEmailFromAddress("library@example.com");
        settings.setEmailFromName("Library");
        lenient().when(globalSettingsService.settingsForEmail()).thenReturn(settings);

        OutgoingEmail outgoingEmail = new OutgoingEmail(branchRepository);
        ReflectionTestUtils.setField(outgoingEmail, "externalBaseUrl", "https://library.example");
        service = new MailingListService(
                subscriptions, catalog, globalSettingsService, outgoingEmail, List.of(sender));
    }

    @Test
    void signupSendsOneImmediateIssueAndSkipsTheSameScheduledOccurrence() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, MailingListWindows.ZONE);
        InstantWindow window = MailingListWindows.catalogAdded(
                MailingFrequency.DAILY, SendOrigin.SIGNUP, now.toLocalDate());
        Book book = new Book();
        book.setId(9L);
        book.setTitle("Tom & Jerry");
        book.setDateAddedToLibrary(window.startUtcInclusive());
        when(catalog.addedBetween(any(), any())).thenReturn(List.of(book));

        MailingListSignupResponse response = service.subscribe(
                "Patron@Example.com",
                List.of(MailingListTopic.NEW_CATALOG_ENTRIES, MailingListTopic.CHILDRENS_FICTION),
                MailingFrequency.DAILY,
                now,
                new Random(1));

        assertEquals(2, response.getResults().size());
        assertEquals(MailingListSignupResponse.CREATED, response.getResults().get(0).getStatus());
        assertEquals(MailingListSignupResponse.CREATED, response.getResults().get(1).getStatus());
        assertEquals(2, sender.sent.size());
        String catalogHtml = sender.sent.get(0).getHtmlBody();
        assertTrue(catalogHtml.contains("Library"));
        assertTrue(catalogHtml.contains("https://library.example"));
        assertTrue(catalogHtml.contains("New Catalog Entries"));
        assertTrue(catalogHtml.contains("Frequency: daily."));
        assertTrue(catalogHtml.contains("unsubscribe and sign up again"));
        assertTrue(catalogHtml.contains("Tom &amp; Jerry"));
        assertTrue(catalogHtml.contains("href=\"https://library.example/books/9\""));
        assertTrue(catalogHtml.contains("God bless,"));
        assertTrue(catalogHtml.contains("-Saint Martin de Porres"));
        int firstUnsub = catalogHtml.indexOf(">Unsubscribe</a>");
        int lastUnsub = catalogHtml.lastIndexOf(">Unsubscribe</a>");
        assertTrue(firstUnsub >= 0 && lastUnsub > firstUnsub);
        assertTrue(lastUnsub > catalogHtml.indexOf("God bless,"));
        assertTrue(lastUnsub > catalogHtml.indexOf("-Saint Martin de Porres"));
        assertEquals(2, count(catalogHtml, ">Unsubscribe</a>"));
        assertTrue(sender.sent.get(1).getHtmlBody().contains(MailingListIssues.NO_BOOKS));
        for (EmailMessage message : sender.sent) {
            String header = message.getHeaders().get(EmailMessage.LIST_UNSUBSCRIBE);
            assertTrue(header.startsWith("<https://library.example/mailing-lists/unsubscribe/"));
            assertTrue(header.endsWith(">"));
            assertEquals("List-Unsubscribe=One-Click", message.getHeaders().get(EmailMessage.LIST_UNSUBSCRIBE_POST));
        }
        assertEquals(64, rows.get(0).getUnsubscribeToken().length());
        assertNotEquals(String.valueOf(rows.get(0).getId()), rows.get(0).getUnsubscribeToken());

        service.sendScheduled(MailingFrequency.DAILY, now.plusHours(6));
        assertEquals(2, sender.sent.size());

        service.sendScheduled(MailingFrequency.DAILY, now.plusDays(1));
        assertEquals(4, sender.sent.size());
    }

    @Test
    void alreadySubscribedDoesNotChangeFrequencyOrSendAgain() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 25, 10, 0, 0, 0, MailingListWindows.ZONE);
        service.subscribe("patron@example.com", List.of(MailingListTopic.ADULT_FICTION),
                MailingFrequency.WEEKLY, now, new Random(1));
        assertEquals(1, sender.sent.size());

        MailingListSignupResponse again = service.subscribe(
                "patron@example.com",
                List.of(MailingListTopic.ADULT_FICTION, MailingListTopic.READ_ALOUD_BOOKS),
                MailingFrequency.DAILY,
                now,
                new Random(1));

        assertEquals(2, sender.sent.size());
        assertEquals(MailingListSignupResponse.ALREADY_SUBSCRIBED, again.getResults().get(0).getStatus());
        assertEquals(MailingFrequency.WEEKLY, again.getResults().get(0).getFrequency());
        assertTrue(again.getResults().get(0).getMessage().contains("Unsubscribe first to change frequency."));
        assertEquals(MailingFrequency.WEEKLY, rows.get(0).getFrequency());
        assertEquals(MailingListSignupResponse.CREATED, again.getResults().get(1).getStatus());
        assertEquals(MailingFrequency.DAILY, rows.get(1).getFrequency());
    }

    @Test
    void unsubscribeIsIdempotentAndDoesNotLogTheToken() {
        ZonedDateTime now = ZonedDateTime.of(2026, 11, 22, 9, 0, 0, 0, MailingListWindows.ZONE);
        service.subscribe("patron@example.com", List.of(MailingListTopic.LIVES_OF_SAINTS),
                MailingFrequency.WEEKLY, now, new Random(1));
        String token = rows.get(0).getUnsubscribeToken();
        Logger logger = (Logger) LoggerFactory.getLogger(MailingListService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MailingListUnsubscribe first = service.unsubscribe(token);
            MailingListUnsubscribe second = service.unsubscribe(token);
            MailingListUnsubscribe missing = service.unsubscribe("not-a-subscription");

            assertEquals(MailingListUnsubscribe.Kind.UNSUBSCRIBED, first.kind());
            assertEquals("Lives of Saints", first.listName());
            assertEquals(MailingListUnsubscribe.Kind.ALREADY, second.kind());
            assertEquals(MailingListUnsubscribe.Kind.UNKNOWN, missing.kind());
            assertFalse(rows.get(0).isActive());
            rows.get(0).setLastOccurrenceKey(null);
            service.sendScheduled(MailingFrequency.WEEKLY, now);
            assertEquals(1, sender.sent.size());

            MailingListSignupResponse resubscribed = service.subscribe(
                    "patron@example.com", List.of(MailingListTopic.LIVES_OF_SAINTS),
                    MailingFrequency.MONTHLY, now, new Random(1));
            assertEquals(MailingListSignupResponse.RESUBSCRIBED, resubscribed.getResults().get(0).getStatus());
            assertEquals(MailingFrequency.MONTHLY, rows.get(0).getFrequency());
            assertEquals(token, rows.get(0).getUnsubscribeToken());
            assertEquals(2, sender.sent.size());

            String logged = appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (a, b) -> a + "\n" + b);
            assertFalse(logged.contains(token));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private static int count(String text, String needle) {
        int found = 0;
        int from = 0;
        while (from <= text.length()) {
            int at = text.indexOf(needle, from);
            if (at < 0) {
                return found;
            }
            found++;
            from = at + needle.length();
        }
        return found;
    }

    private static final class FakeSender implements EmailSender {
        private final List<EmailMessage> sent = new ArrayList<>();

        @Override
        public EmailMethod getMethod() {
            return EmailMethod.LOG;
        }

        @Override
        public boolean isConfigured(GlobalSettings settings) {
            return true;
        }

        @Override
        public String describeStatus(GlobalSettings settings) {
            return "Ready";
        }

        @Override
        public void send(EmailMessage message, GlobalSettings settings) {
            sent.add(message);
        }
    }
}
