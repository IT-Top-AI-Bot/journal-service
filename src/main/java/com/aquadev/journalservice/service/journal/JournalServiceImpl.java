package com.aquadev.journalservice.service.journal;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.kafka.KafkaTopicProperties;
import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkStatus;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.mapper.HomeworkExecutionMapper;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import com.aquadev.journalservice.service.user.group.UserGroupService;
import com.aquadev.journalservice.tracing.HomeworkExecutionSpan;
import com.aquadev.journalservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private static final String AGGREGATE_TYPE = "HomeworkExecution";
    private static final String EVENT_TYPE = "HomeworkExecutionCreated";

    private final JournalClient journalClient;
    private final UserRepository userRepository;
    private final UserGroupService userGroupService;
    private final HomeworkExecutionMapper homeworkExecutionMapper;
    private final HomeworkExecutionRepository homeworkExecutionRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final KafkaTopicProperties kafkaProperties;
    private final HomeworkExecutionSpan homeworkExecutionSpan;

    @Override
    public JournalUserResponse getCurrentUser() {
        return journalClient.getCurrentUser();
    }

    @Override
    public List<JournalCountHomeworkResponse> getCountHomework() {
        List<JournalCountHomeworkResponse> all = journalClient.getCountHomework(null, null);
        if (all == null) {
            return List.of();
        }
        return all.stream()
                .filter(item -> item.counterTypeName() != JournalHomeworkStatus.NOT_COMPLETED)
                .toList();
    }

    @Override
    public Long getCurrentGroupId() {
        return userGroupService.getCurrentGroupId();
    }

    @Override
    public List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type) {
        Long groupIdToUse = userGroupService.getCurrentGroupId();
        return journalClient.getHomeworks(page, status, type, groupIdToUse.intValue(), (Integer) null);
    }

    @Override
    @Transactional
    public HomeworkExecution executeHomework(HomeworkExecutionRequest request) {
        long telegramId = SecurityUtil.getCurrentTelegramUserId();
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(UserNotFoundException::new);

        HomeworkExecution entity = homeworkExecutionMapper.toEntity(request);
        entity.setUser(user);
        HomeworkExecution execution = homeworkExecutionRepository.saveAndFlush(entity);

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

        return execution;
    }

    @Override
    public List<JournalScheduleResponse> getScheduleByDate(LocalDate date) {
        return journalClient.getScheduleByDate(date);
    }

    @Override
    @Cacheable(value = "groupSpecs", cacheManager = "dailyCache", key = "@userGroupServiceImpl.getCurrentGroupId()")
    public List<JournalSpecResponse> getGroupSpecs() {
        return journalClient.getGroupSpecs();
    }
}
