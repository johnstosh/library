/*
 * (c) Copyright 2026 by Muczynski
 */
package com.muczynski.library.repository;

import com.muczynski.library.domain.MailingFrequency;
import com.muczynski.library.domain.MailingListSubscription;
import com.muczynski.library.domain.MailingListTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailingListSubscriptionRepository extends JpaRepository<MailingListSubscription, Long> {

    Optional<MailingListSubscription> findByEmailAndTopic(String email, MailingListTopic topic);

    List<MailingListSubscription> findByEmail(String email);

    Optional<MailingListSubscription> findByUnsubscribeToken(String unsubscribeToken);

    List<MailingListSubscription> findByActiveTrueAndFrequency(MailingFrequency frequency);
}
