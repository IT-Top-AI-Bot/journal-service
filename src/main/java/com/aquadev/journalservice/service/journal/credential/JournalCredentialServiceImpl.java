package com.aquadev.journalservice.service.journal.credential;

import com.aquadev.journalservice.config.kafka.KafkaTopicProperties;
import com.aquadev.journalservice.dto.event.NotificationType;
import com.aquadev.journalservice.dto.event.UserNotificationEvent;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.repository.JournalUserRepository;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JournalCredentialServiceImpl implements JournalCredentialService {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserNotification";

    private final UserRepository userRepository;
    private final JournalUserRepository journalUserRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final KafkaTopicProperties kafkaProperties;

    @Override
    @Transactional
    public void markCredentialsInvalid(Long telegramId) {
        var user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User with telegramId " + telegramId + " not found"));
        var journalUser = user.getJournalUser();
        if (journalUser == null) {
            log.warn("No JournalUser for telegramId={}, skipping credential invalidation", telegramId);
            return;
        }
        if (journalUser.isCredentialsInvalid()) {
            log.debug("Credentials already marked invalid for telegramId={}, skipping duplicate notification", telegramId);
            return;
        }
        journalUser.setCredentialsInvalid(true);
        journalUserRepository.save(journalUser);
        outboxEventPublisher.publish(
                AGGREGATE_TYPE,
                telegramId.toString(),
                EVENT_TYPE,
                kafkaProperties.notificationsTopic(),
                new UserNotificationEvent(telegramId, NotificationType.JOURNAL_CREDENTIALS_INVALID)
        );
        log.info("Credentials marked invalid and notification published for telegramId={}", telegramId);
    }
}
