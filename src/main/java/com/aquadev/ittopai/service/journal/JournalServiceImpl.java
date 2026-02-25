package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.client.journal.JournalClient;
import com.aquadev.ittopai.config.kafka.KafkaTopicProperties;
import com.aquadev.ittopai.config.telegram.TelegramUserContext;
import com.aquadev.ittopai.dto.kafka.HomeworkExecutionEvent;
import com.aquadev.ittopai.dto.request.HomeworkExecutionRequest;
import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;
import com.aquadev.ittopai.exception.domain.user.UserNotFoundException;
import com.aquadev.ittopai.mapper.HomeworkExecutionMapper;
import com.aquadev.ittopai.model.HomeworkExecution;
import com.aquadev.ittopai.model.JournalGroup;
import com.aquadev.ittopai.model.User;
import com.aquadev.ittopai.repository.HomeworkExecutionRepository;
import com.aquadev.ittopai.repository.UserRepository;
import com.aquadev.ittopai.service.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private static final String AGGREGATE_TYPE = "HomeworkExecution";
    private static final String EVENT_TYPE = "HomeworkExecutionCreated";

    private final JournalClient journalClient;
    private final UserRepository userRepository;
    private final HomeworkExecutionMapper homeworkExecutionMapper;
    private final HomeworkExecutionRepository homeworkExecutionRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final KafkaTopicProperties kafkaProperties;

    @Override
    public List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type) {
        Long telegramId = TelegramUserContext.get();
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(UserNotFoundException::new);

        Long groupIdToUse = user.getJournalUser().getJournalGroups().stream()
                .map(JournalGroup::getJournalGroupId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return journalClient.getHomeworks(page, status, type, groupIdToUse.intValue());
    }

    @Override
    @Transactional
    public HomeworkExecution executeHomework(HomeworkExecutionRequest request) {
        HomeworkExecution homeworkExecution = homeworkExecutionMapper.toEntity(request);
        homeworkExecution = homeworkExecutionRepository.saveAndFlush(homeworkExecution);

        outboxEventPublisher.publish(
                AGGREGATE_TYPE,
                homeworkExecution.getId().toString(),
                EVENT_TYPE,
                kafkaProperties.homeworkExecutionTopic(),
                HomeworkExecutionEvent.from(homeworkExecution)
        );

        return homeworkExecution;
    }
}
