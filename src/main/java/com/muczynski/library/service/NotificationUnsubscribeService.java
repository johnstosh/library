/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.service;

import com.muczynski.library.domain.User;
import com.muczynski.library.email.EmailAddresses;
import com.muczynski.library.email.NoticeUnsubscribePaths;
import com.muczynski.library.email.OutgoingEmail;
import com.muczynski.library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

/**
 * Account-level unsubscribe for loan and application notices.
 * Turns {@link User#isReceiveEmails()} off. Does not touch mailing-list rows.
 * The token is an unguessable secret and is not logged.
 */
@Service
public class NotificationUnsubscribeService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationUnsubscribeService.class);

    private final UserRepository userRepository;
    private final OutgoingEmail outgoingEmail;
    private final TransactionTemplate requiresNew;
    private final SecureRandom secureRandom = new SecureRandom();

    public NotificationUnsubscribeService(UserRepository userRepository,
                                          OutgoingEmail outgoingEmail,
                                          PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.outgoingEmail = outgoingEmail;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Absolute unsubscribe URL for a recipient who is a user still receiving notices.
     * An address that is not such a user returns null. Stores a token if one is missing.
     */
    public String urlForAddress(String email) {
        if (!EmailAddresses.isValid(email)) {
            return null;
        }
        List<Long> ids = userRepository.findReceivingIdsByEmailIgnoreCaseOrderByIdAsc(email.trim());
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        String token = ensureToken(ids.get(0));
        if (token == null || token.isBlank()) {
            return null;
        }
        return outgoingEmail.website() + NoticeUnsubscribePaths.PREFIX + token;
    }

    /**
     * Sets receiveEmails false. A second call with the same token stays off.
     * An unknown token does not change any user.
     */
    @Transactional
    public NotificationUnsubscribe unsubscribe(String token) {
        if (token == null || token.isBlank()) {
            return new NotificationUnsubscribe(NotificationUnsubscribe.Kind.UNKNOWN);
        }
        User user = userRepository.findByNotificationUnsubscribeToken(token).orElse(null);
        if (user == null) {
            logger.info("Notification unsubscribe link did not match a user");
            return new NotificationUnsubscribe(NotificationUnsubscribe.Kind.UNKNOWN);
        }
        if (!user.isReceiveEmails()) {
            return new NotificationUnsubscribe(NotificationUnsubscribe.Kind.ALREADY);
        }
        user.setReceiveEmails(false);
        userRepository.save(user);
        logger.info("Turned off email notices for user {}", user.getId());
        return new NotificationUnsubscribe(NotificationUnsubscribe.Kind.UNSUBSCRIBED);
    }

    private String ensureToken(Long userId) {
        return requiresNew.execute(status -> {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return null;
            }
            if (user.getNotificationUnsubscribeToken() != null
                    && !user.getNotificationUnsubscribeToken().isBlank()) {
                return user.getNotificationUnsubscribeToken();
            }
            String created = newToken();
            int updated = userRepository.assignNotificationUnsubscribeTokenIfAbsent(userId, created);
            if (updated == 1) {
                return created;
            }
            User again = userRepository.findById(userId).orElse(null);
            if (again == null) {
                return null;
            }
            return again.getNotificationUnsubscribeToken();
        });
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
