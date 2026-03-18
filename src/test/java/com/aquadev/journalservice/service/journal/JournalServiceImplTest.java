package com.aquadev.journalservice.service.journal;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.config.kafka.KafkaTopicProperties;
import com.aquadev.journalservice.config.telegram.TelegramUserContext;
import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.exception.domain.user.UserNotFoundException;
import com.aquadev.journalservice.mapper.HomeworkExecutionMapper;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceImplTest {

    @Mock JournalClient journalClient;
    @Mock UserRepository userRepository;
    @Mock HomeworkExecutionMapper homeworkExecutionMapper;
    @Mock HomeworkExecutionRepository homeworkExecutionRepository;
    @Mock OutboxEventPublisher outboxEventPublisher;
    @Mock KafkaTopicProperties kafkaProperties;

    @InjectMocks
    JournalServiceImpl journalService;

    private static final long TELEGRAM_ID = 42L;

    // ── getHomeworksForUser ────────────────────────────────────────────────────

    @Test
    void getHomeworksForUser_returnsJournalClientResult() throws Exception {
        User user = buildUserWithGroup(100L);
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));
        List<JournalHomeworkResponse> expected = List.of(mock(JournalHomeworkResponse.class));
        when(journalClient.getHomeworks(1, 3, 1, 100, null)).thenReturn(expected);

        List<JournalHomeworkResponse> result = ScopedValue
                .where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> journalService.getHomeworksForUser(1, 3, 1));

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getHomeworksForUser_userNotFound_throwsUserNotFoundException() {
        // Arrange
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());

        var scopedValue = ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID);

        // Act & Assert
        assertThatThrownBy(() ->
                scopedValue.call(() -> journalService.getHomeworksForUser(1, null, null))
        ).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getHomeworksForUser_noGroup_throwsIllegalArgument() {
        // Arrange
        User user = buildUserWithNoGroups();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        var scopedContext = ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID);

        // Act & Assert
        assertThatThrownBy(() ->
                scopedContext.call(() -> journalService.getHomeworksForUser(1, null, null))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group not found");
    }

    // ── executeHomework ───────────────────────────────────────────────────────

    @Test
    void executeHomework_savesExecutionAndPublishesOutboxEvent() throws Exception {
        User user = buildUserWithGroup(200L);
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        HomeworkExecution execution = buildExecution();
        when(homeworkExecutionMapper.toEntity(any())).thenReturn(execution);
        when(homeworkExecutionRepository.saveAndFlush(execution)).thenReturn(execution);
        when(kafkaProperties.homeworkExecutionTopic()).thenReturn("homework-execution");

        HomeworkExecution result = ScopedValue
                .where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> journalService.executeHomework(new HomeworkExecutionRequest()));

        assertThat(result).isSameAs(execution);
        assertThat(execution.getUser()).isSameAs(user);
        verify(outboxEventPublisher).publish(
                eq("HomeworkExecution"),
                eq(execution.getId().toString()),
                eq("HomeworkExecutionCreated"),
                eq("homework-execution"),
                any()
        );
    }

    @Test
    void executeHomework_userNotFound_throwsUserNotFoundException() {
        // Arrange
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());

        var executionContext = ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID);
        var request = new HomeworkExecutionRequest();

        // Act & Assert
        assertThatThrownBy(() ->
                executionContext.call(() -> journalService.executeHomework(request))
        ).isInstanceOf(UserNotFoundException.class);

        // Дополнительная проверка безопасности
        verifyNoInteractions(outboxEventPublisher);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUserWithGroup(long groupId) {
        JournalGroup group = new JournalGroup();
        group.setJournalGroupId(groupId);
        JournalUser journalUser = new JournalUser();
        journalUser.setJournalGroups(Set.of(group));
        User user = new User();
        user.setTelegramId(TELEGRAM_ID);
        user.setJournalUser(journalUser);
        return user;
    }

    private User buildUserWithNoGroups() {
        JournalUser journalUser = new JournalUser();
        journalUser.setJournalGroups(Set.of());
        User user = new User();
        user.setTelegramId(TELEGRAM_ID);
        user.setJournalUser(journalUser);
        return user;
    }

    private HomeworkExecution buildExecution() {
        HomeworkExecution ex = new HomeworkExecution();
        ex.setId(UUID.randomUUID());
        ex.setHomeworkId(10L);
        ex.setSpecId(1L);
        ex.setTeachId(5L);
        ex.setGroupId(200L);
        ex.setStatus(HomeworkExecutionStatus.PENDING);
        return ex;
    }
}
