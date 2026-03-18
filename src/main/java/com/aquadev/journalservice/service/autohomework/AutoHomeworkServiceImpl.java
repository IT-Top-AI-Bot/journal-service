package com.aquadev.journalservice.service.autohomework;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.journal.JournalApiProperties;
import com.aquadev.journalservice.config.kafka.KafkaTopicProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkStatus;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import com.aquadev.journalservice.repository.UserAutoHomeworkSettingsRepository;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoHomeworkServiceImpl implements AutoHomeworkService {

    private static final String AGGREGATE_TYPE = "HomeworkExecution";
    private static final String EVENT_TYPE = "HomeworkExecutionCreated";

    private final JournalClient journalClient;
    private final UserRepository userRepository;
    private final KafkaTopicProperties kafkaProperties;
    private final JournalApiProperties journalApiProperties;
    private final OutboxEventPublisher outboxEventPublisher;
    private final UserAutoHomeworkSettingsRepository settingsRepository;
    private final HomeworkExecutionRepository homeworkExecutionRepository;

    @Override
    @Transactional(readOnly = true)
    public AutoHomeworkSettingsResponse getSettings(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + telegramId));

        return settingsRepository.findByUserId(user.getId())
                .map(s -> new AutoHomeworkSettingsResponse(s.isEnabled(), s.getLastCheckedAt(), s.getSpecIds()))
                .orElse(new AutoHomeworkSettingsResponse(false, null, Set.of()));
    }

    @Override
    @Transactional
    public AutoHomeworkSettingsResponse updateSettings(Long telegramId, UpdateAutoHomeworkSettingsRequest request) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + telegramId));

        UserAutoHomeworkSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> UserAutoHomeworkSettings.builder().user(user).build());

        settings.setEnabled(request.enabled());
        settings.getSpecIds().clear();

        if (request.specIds() != null) {
            settings.getSpecIds().addAll(request.specIds());
        }

        settings = settingsRepository.save(settings);

        return new AutoHomeworkSettingsResponse(
                settings.isEnabled(),
                settings.getLastCheckedAt(),
                settings.getSpecIds()
        );
    }

    @Override
    @Transactional
    public void checkAndDispatch(UserAutoHomeworkSettings settings) {
        User user = settings.getUser();
        Long telegramId = user.getTelegramId();

        try {
            ScopedValue.where(TelegramUserContext.TG_USER_ID, telegramId).call(() -> {

                Long groupId = user.getJournalUser().getJournalGroups().stream()
                        .map(JournalGroup::getJournalGroupId)
                        .findFirst()
                        .orElse(null);

                if (groupId == null) {
                    log.warn("No group found for user telegramId={}", telegramId);
                    return null;
                }

                Set<Long> specIds = settings.getSpecIds();
                if (specIds == null || specIds.isEmpty()) {
                    log.debug("No specIds configured for telegramId={}, skipping dispatch", telegramId);
                    return null;
                }

                for (Long specId : specIds) {
                    Stream.concat(
                                    getHomeworksBySpec(JournalHomeworkStatus.NOT_COMPLETED, groupId, specId),
                                    getHomeworksBySpec(JournalHomeworkStatus.EXPIRED, groupId, specId)
                            )
                            .filter(hw -> !homeworkExecutionRepository.existsByUserAndHomeworkId(user, hw.id().longValue()))
                            .forEach(hw -> createAndPublish(hw, user));
                }

                settings.setLastCheckedAt(Instant.now());
                settingsRepository.save(settings);

                log.info("Auto homework check completed for user telegramId={}", telegramId);
                return null;
            });
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Auth failure for user telegramId={}, disabling auto-homework: {}", telegramId, e.getMessage());
            settings.setEnabled(false);
            settingsRepository.save(settings);
        } catch (IllegalStateException e) {
            log.error("Illegal state in auto homework check for user telegramId={}: {}", telegramId, e.getMessage());
        } catch (Exception e) {
            log.error("Error in auto homework check for user telegramId={}: {}", telegramId, e.getMessage(), e);
        }
    }

    private Stream<JournalHomeworkResponse> getHomeworksBySpec(JournalHomeworkStatus status, Long groupId, Long specId) {
        List<JournalHomeworkResponse> homeworks = journalClient.getHomeworks(
                1,
                status.getId(),
                0,
                groupId.intValue(),
                specId.intValue()
        );

        return homeworks != null ? homeworks.stream() : Stream.empty();
    }

    private void createAndPublish(JournalHomeworkResponse hw, User user) {

        String filePath = hw.filePath();
        String homeworkUrl = null;

        if (filePath != null) {
            homeworkUrl = filePath.startsWith("http")
                    ? filePath
                    : journalApiProperties.journalUrl() + filePath;
        }

        HomeworkExecution execution = HomeworkExecution.builder()
                .homeworkId(hw.id().longValue())
                .specId(hw.idSpec().longValue())
                .teachId(hw.idTeach().longValue())
                .groupId(hw.idGroup().longValue())
                .teacherFio(hw.fioTeach())
                .theme(hw.theme())
                .completionTime(hw.completionTime())
                .overdueTime(hw.overdueTime())
                .comment(hw.comment())
                .nameSpec(hw.nameSpec())
                .homeworkUrl(homeworkUrl)
                .status(HomeworkExecutionStatus.PENDING)
                .user(user)
                .build();

        execution = homeworkExecutionRepository.saveAndFlush(execution);

        outboxEventPublisher.publish(
                AGGREGATE_TYPE,
                execution.getId().toString(),
                EVENT_TYPE,
                kafkaProperties.homeworkExecutionTopic(),
                new HomeworkExecutionEvent(
                        execution.getId(),
                        execution.getTheme(),
                        execution.getSpecId(),
                        execution.getStatus(),
                        execution.getComment(),
                        execution.getGroupId(),
                        execution.getTeachId(),
                        execution.getNameSpec(),
                        execution.getCreatedAt(),
                        execution.getHomeworkId(),
                        execution.getTeacherFio(),
                        execution.getHomeworkUrl(),
                        execution.getOverdueTime(),
                        execution.getCompletionTime(),
                        null
                )
        );

        log.debug(
                "Dispatched auto homework execution id={} homeworkId={} for user telegramId={}",
                execution.getId(),
                execution.getHomeworkId(),
                user.getTelegramId()
        );
    }
}
