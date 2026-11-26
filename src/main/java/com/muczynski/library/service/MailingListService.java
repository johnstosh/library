/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.EmailMethod;
import com.muczynski.library.domain.GlobalSettings;
import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.domain.MailingListSubscription;
import com.muczynski.library.domain.MailingListTopic;
import com.muczynski.library.dto.MailingListSignupResponse;
import com.muczynski.library.email.EmailMessage;
import com.muczynski.library.email.EmailSender;
import com.muczynski.library.email.OutgoingEmail;
import com.muczynski.library.repository.MailingListSubscriptionRepository;
import com.muczynski.library.service.MailingListWindows.InstantWindow;
import com.muczynski.library.service.MailingListWindows.SendOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Signs patrons up, sends the previous-period issue at once, and sends the
 * scheduled issue at most once per Eastern occurrence.
 */
@Service
public class MailingListService {

    private static final Logger logger = LoggerFactory.getLogger(MailingListService.class);

    private final MailingListSubscriptionRepository subscriptions;
    private final MailingListCatalog catalog;
    private final GlobalSettingsService globalSettingsService;
    private final OutgoingEmail outgoingEmail;
    private final Map<EmailMethod, EmailSender> senders;
    private final SecureRandom secureRandom = new SecureRandom();

    public MailingListService(MailingListSubscriptionRepository subscriptions,
                              MailingListCatalog catalog,
                              GlobalSettingsService globalSettingsService,
                              OutgoingEmail outgoingEmail,
                              List<EmailSender> senderList) {
        this.subscriptions = subscriptions;
        this.catalog = catalog;
        this.globalSettingsService = globalSettingsService;
        this.outgoingEmail = outgoingEmail;
        this.senders = new EnumMap<>(EmailMethod.class);
        for (EmailSender sender : senderList) {
            this.senders.put(sender.getMethod(), sender);
        }
    }

    public MailingListSignupResponse subscribe(String email, List<MailingListTopic> lists, MailingFrequency frequency) {
        return subscribe(email, lists, frequency, ZonedDateTime.now(MailingListWindows.ZONE), ThreadLocalRandom.current());
    }

    public MailingListSignupResponse subscribe(String email,
                                               List<MailingListTopic> lists,
                                               MailingFrequency frequency,
                                               ZonedDateTime now,
                                               Random random) {
        String normalized = normalizeEmail(email);
        MailingListSignupResponse response = new MailingListSignupResponse();
        for (MailingListTopic topic : distinct(lists)) {
            response.getResults().add(subscribeOne(normalized, topic, frequency, now, random));
        }
        return response;
    }

    public void sendScheduled(MailingFrequency frequency) {
        sendScheduled(frequency, ZonedDateTime.now(MailingListWindows.ZONE));
    }

    public void sendScheduled(MailingFrequency frequency, ZonedDateTime now) {
        ZonedDateTime eastern = now.withZoneSameInstant(MailingListWindows.ZONE);
        LocalDate today = eastern.toLocalDate();
        String key = MailingListWindows.occurrenceKey(frequency, today);
        List<MailingListSubscription> due = subscriptions.findByActiveTrueAndFrequency(frequency);
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        for (MailingListSubscription subscription : due) {
            if (key.equals(subscription.getLastOccurrenceKey())) {
                skipped++;
                continue;
            }
            try {
                if (deliver(subscription, eastern, ThreadLocalRandom.current(), SendOrigin.SCHEDULED)) {
                    subscription.setLastOccurrenceKey(key);
                    subscriptions.save(subscription);
                    sent++;
                } else {
                    failed++;
                }
            } catch (RuntimeException ex) {
                failed++;
                logger.warn("Scheduled mailing list send failed for {} {}: {}",
                        subscription.getEmail(), subscription.getTopic(), safeError(ex));
            }
        }
        logger.info("Scheduled {} mailing list for {}: sent {}, already sent {}, not sent {}",
                frequency.word(), today, sent, skipped, failed);
    }

    /**
     * Unsubscribes immediately. A second call with the same token reports that
     * the patron is already unsubscribed. The token is not logged.
     */
    @Transactional
    public MailingListUnsubscribe unsubscribe(String token) {
        if (token == null || token.isBlank()) {
            return new MailingListUnsubscribe(MailingListUnsubscribe.Kind.UNKNOWN, "");
        }
        Optional<MailingListSubscription> found = subscriptions.findByUnsubscribeToken(token);
        if (found.isEmpty()) {
            logger.info("Unsubscribe link did not match a subscription");
            return new MailingListUnsubscribe(MailingListUnsubscribe.Kind.UNKNOWN, "");
        }
        MailingListSubscription subscription = found.get();
        String listName = subscription.getTopic().getDisplayName();
        if (!subscription.isActive()) {
            return new MailingListUnsubscribe(MailingListUnsubscribe.Kind.ALREADY, listName);
        }
        subscription.setActive(false);
        subscription.setUnsubscribedAt(LocalDateTime.now(ZoneOffset.UTC));
        subscriptions.save(subscription);
        logger.info("Unsubscribed a patron from {}", listName);
        return new MailingListUnsubscribe(MailingListUnsubscribe.Kind.UNSUBSCRIBED, listName);
    }

    /**
     * Move this address's list rows onto the new account email.
     * A blank new address turns the old rows off so mail stops.
     * A list already stored at the new address stays; the old row is turned off
     * when it was active, and an inactive destination is turned back on.
     */
    @Transactional
    public void moveEmail(String oldEmail, String newEmail) {
        if (oldEmail == null || oldEmail.isBlank()) {
            return;
        }
        if (newEmail == null || newEmail.isBlank()) {
            deactivateEmail(oldEmail);
            return;
        }
        if (oldEmail.equals(newEmail)) {
            return;
        }
        List<MailingListSubscription> rows = new ArrayList<>(subscriptions.findByEmail(oldEmail));
        for (MailingListSubscription row : rows) {
            Optional<MailingListSubscription> clash = subscriptions.findByEmailAndTopic(newEmail, row.getTopic());
            if (clash.isPresent() && !sameRow(clash.get(), row)) {
                keepListAtNewAddress(row, clash.get());
                continue;
            }
            row.setEmail(newEmail);
            subscriptions.save(row);
        }
    }

    private void keepListAtNewAddress(MailingListSubscription oldRow, MailingListSubscription kept) {
        if (oldRow.isActive() && !kept.isActive()) {
            kept.setActive(true);
            kept.setFrequency(oldRow.getFrequency());
            kept.setUnsubscribedAt(null);
            subscriptions.save(kept);
        }
        if (oldRow.isActive()) {
            oldRow.setActive(false);
            oldRow.setUnsubscribedAt(LocalDateTime.now(ZoneOffset.UTC));
            subscriptions.save(oldRow);
        }
    }

    private void deactivateEmail(String email) {
        for (MailingListSubscription row : subscriptions.findByEmail(email)) {
            if (!row.isActive()) {
                continue;
            }
            row.setActive(false);
            row.setUnsubscribedAt(LocalDateTime.now(ZoneOffset.UTC));
            subscriptions.save(row);
        }
    }

    private static boolean sameRow(MailingListSubscription left, MailingListSubscription right) {
        if (left == right) {
            return true;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private MailingListSignupResponse.Line subscribeOne(String email,
                                                        MailingListTopic topic,
                                                        MailingFrequency frequency,
                                                        ZonedDateTime now,
                                                        Random random) {
        MailingListSignupResponse.Line line = new MailingListSignupResponse.Line();
        line.setList(topic);
        line.setDisplayName(topic.getDisplayName());
        Optional<MailingListSubscription> existing = subscriptions.findByEmailAndTopic(email, topic);
        if (existing.isPresent() && existing.get().isActive()) {
            MailingListSubscription current = existing.get();
            line.setFrequency(current.getFrequency());
            line.setStatus(MailingListSignupResponse.ALREADY_SUBSCRIBED);
            line.setMessage("Already subscribed to " + topic.getDisplayName()
                    + ". Unsubscribe first to change frequency.");
            line.setSent(false);
            return line;
        }

        boolean reactivated = existing.isPresent();
        MailingListSubscription subscription = existing.orElseGet(MailingListSubscription::new);
        if (!reactivated) {
            subscription.setEmail(email);
            subscription.setTopic(topic);
            subscription.setUnsubscribeToken(newToken());
        } else if (subscription.getUnsubscribeToken() == null || subscription.getUnsubscribeToken().isBlank()) {
            subscription.setUnsubscribeToken(newToken());
        }
        subscription.setFrequency(frequency);
        subscription.setActive(true);
        subscription.setUnsubscribedAt(null);
        subscription.setSubscribedAt(LocalDateTime.now(ZoneOffset.UTC));
        subscription.setLastOccurrenceKey(null);
        try {
            subscription = subscriptions.save(subscription);
        } catch (DataIntegrityViolationException ex) {
            Optional<MailingListSubscription> raced = subscriptions.findByEmailAndTopic(email, topic);
            if (raced.isPresent() && raced.get().isActive()) {
                line.setFrequency(raced.get().getFrequency());
                line.setStatus(MailingListSignupResponse.ALREADY_SUBSCRIBED);
                line.setMessage("Already subscribed to " + topic.getDisplayName()
                        + ". Unsubscribe first to change frequency.");
                line.setSent(false);
                return line;
            }
            throw ex;
        }

        line.setFrequency(frequency);
        line.setStatus(reactivated ? MailingListSignupResponse.RESUBSCRIBED : MailingListSignupResponse.CREATED);
        ZonedDateTime eastern = now.withZoneSameInstant(MailingListWindows.ZONE);
        try {
            boolean sent = deliver(subscription, eastern, random, SendOrigin.SIGNUP);
            line.setSent(sent);
            if (sent) {
                subscription.setLastOccurrenceKey(
                        MailingListWindows.occurrenceKey(frequency, eastern.toLocalDate()));
                subscriptions.save(subscription);
            } else {
                line.setMessage("Subscribed, but the first issue could not be sent.");
            }
        } catch (RuntimeException ex) {
            line.setSent(false);
            line.setMessage("Subscribed, but the first issue could not be sent.");
            logger.warn("Mailing list signup send failed for {}: {}", topic, safeError(ex));
        }
        return line;
    }

    private boolean deliver(MailingListSubscription subscription,
                            ZonedDateTime easternNow,
                            Random random,
                            SendOrigin origin) {
        GlobalSettings settings = globalSettingsService.settingsForEmail();
        EmailMethod method = settings.getEmailMethod() != null ? settings.getEmailMethod() : EmailMethod.DISABLED;
        if (method == EmailMethod.DISABLED) {
            logger.info("Mailing list issue not sent; email method is disabled");
            return false;
        }
        EmailSender sender = senders.get(method);
        if (sender == null || !sender.isConfigured(settings)) {
            logger.info("Mailing list issue not sent; {}",
                    sender == null ? "no sender" : sender.describeStatus(settings));
            return false;
        }
        EmailMessage message = messageFor(subscription, easternNow, random, origin, settings);
        sender.send(message, settings);
        return true;
    }

    private EmailMessage messageFor(MailingListSubscription subscription,
                                   ZonedDateTime easternNow,
                                   Random random,
                                   SendOrigin origin,
                                   GlobalSettings settings) {
        LocalDate today = easternNow.toLocalDate();
        String site = outgoingEmail.website();
        MailingListIssues.Issue issue = issueFor(subscription, origin, today, site, random);
        String unsubscribeUrl = site + "/mailing-lists/unsubscribe/" + subscription.getUnsubscribeToken();

        EmailMessage message = new EmailMessage();
        message.getTo().add(subscription.getEmail());
        if (settings.getEmailFromAddress() != null) {
            message.setFromAddress(settings.getEmailFromAddress().trim());
        }
        if (settings.getEmailFromName() != null) {
            message.setFromName(settings.getEmailFromName().trim());
        }
        message.setEvent("library.mailing-list");
        message.setSubject(subscription.getTopic().getDisplayName() + " (" + subscription.getFrequency().word() + ")");
        message.getEventPayload().put("list", subscription.getTopic().name());
        message.getEventPayload().put("frequency", subscription.getFrequency().name());
        message.setListUnsubscribe(unsubscribeUrl);
        outgoingEmail.composeMailingList(
                message,
                subscription.getTopic().getDisplayName(),
                subscription.getFrequency().sentence(),
                issue.text(),
                issue.html(),
                unsubscribeUrl);
        return message;
    }

    private MailingListIssues.Issue issueFor(MailingListSubscription subscription,
                                            SendOrigin origin,
                                            LocalDate today,
                                            String site,
                                            Random random) {
        MailingFrequency frequency = subscription.getFrequency();
        MailingListTopic topic = subscription.getTopic();
        return switch (topic) {
            case LIVES_OF_SAINTS -> MailingListIssues.saints(
                    catalog.feastBooks(),
                    MailingListWindows.saintDays(frequency, origin, today),
                    site);
            case NEW_CATALOG_ENTRIES -> {
                InstantWindow window = MailingListWindows.catalogAdded(frequency, origin, today);
                yield MailingListIssues.catalog(
                        catalog.addedBetween(window.startUtcInclusive(), window.endUtcExclusive()),
                        window,
                        site);
            }
            case CHILDRENS_FICTION, READ_ALOUD_BOOKS, ADULT_FICTION -> MailingListIssues.oneRandom(
                    catalog.tagged(topic.requiredTags()),
                    topic.requiredTags(),
                    site,
                    random);
        };
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static List<MailingListTopic> distinct(List<MailingListTopic> lists) {
        if (lists == null || lists.isEmpty()) {
            return List.of();
        }
        Set<MailingListTopic> unique = new LinkedHashSet<>();
        for (MailingListTopic topic : lists) {
            if (topic != null) {
                unique.add(topic);
            }
        }
        return new ArrayList<>(unique);
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeError(Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return message.replaceAll("(/mailing-lists/unsubscribe/)[^\\s\"'<>]+", "$1[redacted]");
    }
}
