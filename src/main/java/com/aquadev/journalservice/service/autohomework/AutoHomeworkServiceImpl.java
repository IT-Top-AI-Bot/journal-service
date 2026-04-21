package com.aquadev.journalservice.service.autohomework;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.journal.JournalApiProperties;
import com.aquadev.journalservice.config.kafka.KafkaTopicProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.request.HomeworkType;
import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkStatus;
import com.aquadev.journalservice.exception.domain.journal.JournalCredentialsInvalidException;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import com.aquadev.journalservice.repository.JournalUserRepository;
import com.aquadev.journalservice.repository.UserAutoHomeworkSettingsRepository;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.journal.credential.JournalCredentialService;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import com.aquadev.journalservice.tracing.HomeworkExecutionSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoHomeworkServiceImpl implements AutoHomeworkService {

    private static final String AGGREGATE_TYPE = "HomeworkExecution";
    private static final String EVENT_TYPE = "HomeworkExecutionCreated";

    private static final Set<JournalHomeworkStatus> RELEVANT_STATUSES = EnumSet.of(
            JournalHomeworkStatus.NOT_COMPLETED,
            JournalHomeworkStatus.EXPIRED,
            JournalHomeworkStatus.DELETED_BY_TEACHER
    );

    private final JournalClient journalClient;
    private final UserRepository userRepository;
    private final JournalUserRepository journalUserRepository;
    private final KafkaTopicProperties kafkaProperties;
    private final JournalApiProperties journalApiProperties;
    private final OutboxEventPublisher outboxEventPublisher;
    private final HomeworkExecutionSpan homeworkExecutionSpan;
    private final JournalCredentialService journalCredentialService;
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
        if (request.enabled() && user.getJournalUser() != null) {
            user.getJournalUser().setCredentialsInvalid(false);
            journalUserRepository.save(user.getJournalUser());
        }
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
                doCheckAndDispatch(settings, user, telegramId);
                return null;
            });
        } catch (HttpClientErrorException.TooManyRequests _) {
            log.warn("Rate limit hit for user telegramId={}, will retry on next check", telegramId);
        } catch (HttpClientErrorException.UnprocessableContent e) {
            String body = e.getResponseBodyAsString();
            if (body.contains("Неверный логин или пароль")) {
                log.warn("Invalid journal credentials for telegramId={}, disabling auto-homework", telegramId);
                journalCredentialService.markCredentialsInvalid(telegramId);
            } else {
                log.error("Unexpected 422 in auto homework check for telegramId={}: {}", telegramId, e.getMessage(), e);
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Auth failure for user telegramId={}, will retry on next check: {}", telegramId, e.getMessage());
        } catch (JournalCredentialsInvalidException _) {
            log.warn("Invalid journal credentials for telegramId={}, marking invalid", telegramId);
            journalCredentialService.markCredentialsInvalid(telegramId);
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.startsWith("Reauth required") || msg.startsWith("Missing credentials"))) {
                log.warn("Unrecoverable auth state for telegramId={}: {}", telegramId, msg);
                journalCredentialService.markCredentialsInvalid(telegramId);
            } else {
                log.error("Illegal state in auto homework check for user telegramId={}: {}", telegramId, msg);
            }
        } catch (Exception e) {
            log.error("Error in auto homework check for user telegramId={}: {}", telegramId, e.getMessage(), e);
        }
    }

    private void doCheckAndDispatch(UserAutoHomeworkSettings settings, User user, Long telegramId) {
        Long groupId = resolveGroupId(user, telegramId);
        if (groupId == null) return;

        Set<Long> specIds = settings.getSpecIds();
        if (specIds == null || specIds.isEmpty()) {
            log.debug("No specIds configured for telegramId={}, skipping dispatch", telegramId);
            return;
        }

        for (HomeworkType type : HomeworkType.values()) {
            if (!hasRelevantForType(type, groupId)) {
                log.debug("No relevant homeworks of type={} for groupId={}, skipping", type, groupId);
                continue;
            }
            dispatchByType(type, groupId, specIds, user);
        }

        settings.setLastCheckedAt(Instant.now());
        settingsRepository.save(settings);

        log.info("Auto homework check completed for user telegramId={}", telegramId);
    }

    private Long resolveGroupId(User user, Long telegramId) {
        Long groupId = user.getJournalUser().getJournalGroups().stream()
                .map(JournalGroup::getJournalGroupId)
                .findFirst()
                .orElse(null);
        if (groupId == null) {
            log.warn("No group found for user telegramId={}", telegramId);
        }
        return groupId;
    }

    private boolean hasRelevantForType(HomeworkType type, Long groupId) {
        List<JournalCountHomeworkResponse> counts = journalClient.getCountHomework(type.getId(), groupId.intValue());
        return counts != null && counts.stream()
                .anyMatch(r -> RELEVANT_STATUSES.contains(r.counterTypeName())
                        && r.counter() != null && r.counter() > 0);
    }

    private static final int HOMEWORK_MAX_AGE_MONTHS = 6;

    private void dispatchByType(HomeworkType type, Long groupId, Set<Long> specIds, User user) {
        List<Integer> specIdList = specIds.stream().map(Long::intValue).toList();
        LocalDate cutoff = LocalDate.now().minusMonths(HOMEWORK_MAX_AGE_MONTHS);
        RELEVANT_STATUSES.stream()
                .flatMap(status -> fetchHomeworks(status, type, groupId, specIdList))
                .filter(hw -> hw.creationTime() != null && !hw.creationTime().isBefore(cutoff))
                .forEach(hw -> createOrRetry(hw, user));
    }

    private Stream<JournalHomeworkResponse> fetchHomeworks(JournalHomeworkStatus status, HomeworkType type, Long groupId, List<Integer> specIds) {
        List<JournalHomeworkResponse> homeworks = journalClient.getHomeworks(
                1, status.getId(), type.getId(), groupId.intValue(), specIds
        );
        return homeworks != null ? homeworks.stream() : Stream.empty();
    }

    private static final int MAX_RETRY_COUNT = 3;

    private void createOrRetry(JournalHomeworkResponse hw, User user) {
        Long homeworkId = hw.id().longValue();

        homeworkExecutionRepository.findByUserAndHomeworkId(user, homeworkId)
                .ifPresentOrElse(
                        existing -> retryIfEligible(existing, user),
                        () -> createAndPublish(hw, user)
                );
    }

    private void retryIfEligible(HomeworkExecution existing, User user) {
        if (existing.getStatus() != HomeworkExecutionStatus.FAILED) {
            log.debug("Skipping homeworkId={} — status={}", existing.getHomeworkId(), existing.getStatus());
            return;
        }
        if (existing.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Skipping homeworkId={} — max retries ({}) exhausted",
                    existing.getHomeworkId(), MAX_RETRY_COUNT);
            return;
        }

        existing.setStatus(HomeworkExecutionStatus.PENDING);
        existing.setRetryCount(existing.getRetryCount() + 1);
        existing.setResultS3Key(null);
        existing.setResultText(null);
        existing.setCompletedAt(null);
        HomeworkExecution saved = homeworkExecutionRepository.saveAndFlush(existing);

        log.info("Retrying homeworkId={} attempt={}/{} for user telegramId={}",
                saved.getHomeworkId(), saved.getRetryCount(), MAX_RETRY_COUNT, user.getTelegramId());

        publishEvent(saved);
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

        log.debug("Dispatched new execution id={} homeworkId={} for user telegramId={}",
                execution.getId(), execution.getHomeworkId(), user.getTelegramId());

        publishEvent(execution);
    }

    private void publishEvent(HomeworkExecution execution) {
        homeworkExecutionSpan.run(execution, () ->
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
                                execution.getCompletionTime()
                        )
                )
        );
    }
}
