package com.aquadev.journalservice.service.autohomework;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalHomeworkQueryClient;
import com.aquadev.journalservice.config.journal.JournalApiProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.request.HomeworkType;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkStatus;
import com.aquadev.journalservice.exception.domain.journal.JournalAuthenticationException;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import com.aquadev.journalservice.repository.UserAutoHomeworkSettingsRepository;
import com.aquadev.journalservice.service.execution.HomeworkExecutionEventPublisher;
import com.aquadev.journalservice.service.journal.credential.JournalCredentialService;
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
public class AutoHomeworkDispatchServiceImpl implements AutoHomeworkDispatchService {

    private static final Set<JournalHomeworkStatus> RELEVANT_STATUSES = EnumSet.of(
            JournalHomeworkStatus.NOT_COMPLETED,
            JournalHomeworkStatus.EXPIRED,
            JournalHomeworkStatus.DELETED_BY_TEACHER
    );
    private static final int HOMEWORK_MAX_AGE_MONTHS = 6;
    private static final int MAX_RETRY_COUNT = 3;

    private final JournalHomeworkQueryClient homeworkQueryClient;
    private final JournalApiProperties journalApiProperties;
    private final JournalCredentialService journalCredentialService;
    private final UserAutoHomeworkSettingsRepository settingsRepository;
    private final HomeworkExecutionRepository homeworkExecutionRepository;
    private final HomeworkExecutionEventPublisher homeworkExecutionEventPublisher;

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
        } catch (JournalAuthenticationException exception) {
            log.warn("Invalid journal credentials for telegramId={}, marking invalid: {}",
                    telegramId, exception.getClass().getSimpleName());
            journalCredentialService.markCredentialsInvalid(telegramId);
        } catch (Exception exception) {
            log.error("Error in auto homework check for user telegramId={}: {}",
                    telegramId, exception.getMessage(), exception);
        }
    }

    private void doCheckAndDispatch(UserAutoHomeworkSettings settings, User user, Long telegramId) {
        Long groupId = resolveGroupId(user, telegramId);
        if (groupId == null) {
            return;
        }

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
        List<JournalCountHomeworkResponse> counts = homeworkQueryClient.getCountHomework(type.getId(), groupId.intValue());
        return counts != null && counts.stream()
                .anyMatch(response -> RELEVANT_STATUSES.contains(response.counterTypeName())
                        && response.counter() != null
                        && response.counter() > 0);
    }

    private void dispatchByType(HomeworkType type, Long groupId, Set<Long> specIds, User user) {
        List<Integer> specIdList = specIds.stream().map(Long::intValue).toList();
        LocalDate cutoff = LocalDate.now().minusMonths(HOMEWORK_MAX_AGE_MONTHS);
        RELEVANT_STATUSES.stream()
                .flatMap(status -> fetchHomeworks(status, type, groupId, specIdList))
                .filter(homework -> homework.creationTime() != null && !homework.creationTime().isBefore(cutoff))
                .forEach(homework -> createOrRetry(homework, user));
    }

    private Stream<JournalHomeworkResponse> fetchHomeworks(
            JournalHomeworkStatus status,
            HomeworkType type,
            Long groupId,
            List<Integer> specIds
    ) {
        List<JournalHomeworkResponse> homeworks = homeworkQueryClient.getHomeworks(
                1,
                status.getId(),
                type.getId(),
                groupId.intValue(),
                specIds
        );
        return homeworks != null ? homeworks.stream() : Stream.empty();
    }

    private void createOrRetry(JournalHomeworkResponse homework, User user) {
        Long homeworkId = homework.id().longValue();
        homeworkExecutionRepository.findByUserAndHomeworkId(user, homeworkId)
                .ifPresentOrElse(
                        this::retryIfEligible,
                        () -> createAndPublish(homework, user)
                );
    }

    private void retryIfEligible(HomeworkExecution existing) {
        if (existing.getStatus() != HomeworkExecutionStatus.FAILED) {
            log.debug("Skipping homeworkId={} - status={}", existing.getHomeworkId(), existing.getStatus());
            return;
        }
        if (existing.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Skipping homeworkId={} - max retries ({}) exhausted",
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
                saved.getHomeworkId(), saved.getRetryCount(), MAX_RETRY_COUNT, saved.getUser().getTelegramId());
        homeworkExecutionEventPublisher.publishCreated(saved);
    }

    private void createAndPublish(JournalHomeworkResponse homework, User user) {
        String filePath = homework.filePath();
        String homeworkUrl = null;

        if (filePath != null) {
            homeworkUrl = filePath.startsWith("http")
                    ? filePath
                    : journalApiProperties.journalUrl() + filePath;
        }

        HomeworkExecution execution = HomeworkExecution.builder()
                .homeworkId(homework.id().longValue())
                .specId(homework.idSpec().longValue())
                .teachId(homework.idTeach().longValue())
                .groupId(homework.idGroup().longValue())
                .teacherFio(homework.fioTeach())
                .theme(homework.theme())
                .completionTime(homework.completionTime())
                .overdueTime(homework.overdueTime())
                .comment(homework.comment())
                .nameSpec(homework.nameSpec())
                .homeworkUrl(homeworkUrl)
                .status(HomeworkExecutionStatus.PENDING)
                .user(user)
                .build();

        HomeworkExecution saved = homeworkExecutionRepository.saveAndFlush(execution);
        log.debug("Dispatched new execution id={} homeworkId={} for user telegramId={}",
                saved.getId(), saved.getHomeworkId(), user.getTelegramId());
        homeworkExecutionEventPublisher.publishCreated(saved);
    }
}
