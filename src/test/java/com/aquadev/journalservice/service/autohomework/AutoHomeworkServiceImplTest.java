package com.aquadev.journalservice.service.autohomework;

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
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.model.UserAutoHomeworkSettings;
import com.aquadev.journalservice.repository.HomeworkExecutionRepository;
import com.aquadev.journalservice.repository.UserAutoHomeworkSettingsRepository;
import com.aquadev.journalservice.repository.UserRepository;
import com.aquadev.journalservice.service.outbox.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoHomeworkServiceImplTest {

    @Mock
    JournalClient journalClient;
    @Mock
    UserRepository userRepository;
    @Mock
    HomeworkExecutionRepository homeworkExecutionRepository;
    @Mock
    UserAutoHomeworkSettingsRepository settingsRepository;
    @Mock
    OutboxEventPublisher outboxEventPublisher;
    @Mock
    KafkaTopicProperties kafkaProperties;
    @Mock
    JournalApiProperties journalApiProperties;

    @InjectMocks
    AutoHomeworkServiceImpl service;

    private static final long TELEGRAM_ID = 100L;

    // ── getSettings ───────────────────────────────────────────────────────────

    @Test
    void getSettings_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSettings(TELEGRAM_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getSettings_noSettings_returnsDisabledDefault() {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));
        when(settingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        AutoHomeworkSettingsResponse response = service.getSettings(TELEGRAM_ID);

        assertThat(response.enabled()).isFalse();
        assertThat(response.specIds()).isEmpty();
        assertThat(response.lastCheckedAt()).isNull();
    }

    @Test
    void getSettings_settingsExist_returnsCorrectData() {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        UserAutoHomeworkSettings settings = UserAutoHomeworkSettings.builder()
                .user(user).enabled(true).specIds(Set.of(1L, 2L))
                .build();
        settings.setLastCheckedAt(Instant.parse("2025-01-01T00:00:00Z"));
        when(settingsRepository.findByUserId(user.getId())).thenReturn(Optional.of(settings));

        AutoHomeworkSettingsResponse response = service.getSettings(TELEGRAM_ID);

        assertThat(response.enabled()).isTrue();
        assertThat(response.specIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.lastCheckedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    }

    // ── updateSettings ────────────────────────────────────────────────────────

    @Test
    void updateSettings_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());

        UpdateAutoHomeworkSettingsRequest request =
                new UpdateAutoHomeworkSettingsRequest(true, Set.of(1L));

        assertThatThrownBy(() -> service.updateSettings(TELEGRAM_ID, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateSettings_createsNewSettings_whenNoneExist() {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));
        when(settingsRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        UserAutoHomeworkSettings saved = UserAutoHomeworkSettings.builder()
                .user(user).enabled(true).specIds(new HashSet<>(Set.of(5L))).build();
        when(settingsRepository.save(any())).thenReturn(saved);

        AutoHomeworkSettingsResponse response = service.updateSettings(TELEGRAM_ID,
                new UpdateAutoHomeworkSettingsRequest(true, Set.of(5L)));

        assertThat(response.enabled()).isTrue();
        assertThat(response.specIds()).contains(5L);
    }

    @Test
    void updateSettings_updatesExistingSettings_replacingSpecIds() {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        UserAutoHomeworkSettings existing = UserAutoHomeworkSettings.builder()
                .user(user).enabled(false).specIds(new HashSet<>(Set.of(1L, 2L))).build();
        when(settingsRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        UserAutoHomeworkSettings saved = UserAutoHomeworkSettings.builder()
                .user(user).enabled(true).specIds(new HashSet<>(Set.of(3L))).build();
        when(settingsRepository.save(any())).thenReturn(saved);

        service.updateSettings(TELEGRAM_ID, new UpdateAutoHomeworkSettingsRequest(true, Set.of(3L)));

        // Old specIds should be cleared and new ones added
        assertThat(existing.getSpecIds()).containsExactly(3L);
        assertThat(existing.isEnabled()).isTrue();
    }

    @Test
    void updateSettings_nullSpecIds_clearsExistingSpecIds() {
        User user = buildUser();
        when(userRepository.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(user));

        UserAutoHomeworkSettings existing = UserAutoHomeworkSettings.builder()
                .user(user).enabled(true).specIds(new HashSet<>(Set.of(1L, 2L))).build();
        when(settingsRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any())).thenReturn(existing);

        service.updateSettings(TELEGRAM_ID, new UpdateAutoHomeworkSettingsRequest(false, null));

        assertThat(existing.getSpecIds()).isEmpty();
    }

    // ── checkAndDispatch ──────────────────────────────────────────────────────

    @Test
    void checkAndDispatch_noGroup_skipsDispatch() {
        User user = buildUserWithJournalUser(Set.of()); // no groups
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L)); // Fix: Added specId so it passes early exit

        service.checkAndDispatch(settings);

        verifyNoInteractions(journalClient, outboxEventPublisher);
    }

    @Test
    void checkAndDispatch_dispatchesNewHomework() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L)); // Fix: Added specId=5L

        JournalHomeworkResponse hw = makeHomework(1, 5, 3, 10, "http://example.com/hw.pdf");
        when(journalClient.getHomeworks(1, JournalHomeworkStatus.NOT_COMPLETED.getId(), 0, 10, 5))
                .thenReturn(List.of(hw));
        when(journalClient.getHomeworks(1, JournalHomeworkStatus.EXPIRED.getId(), 0, 10, 5))
                .thenReturn(List.of());
        when(homeworkExecutionRepository.existsByUserAndHomeworkId(user, 1L)).thenReturn(false);
        when(homeworkExecutionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            HomeworkExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(kafkaProperties.homeworkExecutionTopic()).thenReturn("topic");
        when(journalApiProperties.journalUrl()).thenReturn("http://api");

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verify(outboxEventPublisher).publish(any(), any(), any(), any(), any());
        verify(settingsRepository).save(settings);
        assertThat(settings.getLastCheckedAt()).isNotNull();
    }

    @Test
    void checkAndDispatch_homeworkAlreadyExists_skipsDispatch() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L)); // Fix: Added specId=5L

        JournalHomeworkResponse hw = makeHomework(1, 5, 3, 10, null);
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.NOT_COMPLETED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(hw));
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.EXPIRED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(homeworkExecutionRepository.existsByUserAndHomeworkId(user, 1L)).thenReturn(true);

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verifyNoInteractions(outboxEventPublisher);
    }

    @Test
    void checkAndDispatch_specIdFilterApplied_onlyMatchingSpecIdsDispatched() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        // Only specId=5 allowed — API is called per specId, so only spec-5 homeworks returned
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L));

        JournalHomeworkResponse matchingHw = makeHomework(1, 5, 3, 10, null);
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.NOT_COMPLETED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(matchingHw));
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.EXPIRED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(homeworkExecutionRepository.existsByUserAndHomeworkId(user, 1L)).thenReturn(false);
        when(homeworkExecutionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            HomeworkExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(kafkaProperties.homeworkExecutionTopic()).thenReturn("topic");
        when(journalApiProperties.journalUrl()).thenReturn("http://api");

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verify(outboxEventPublisher, times(1)).publish(any(), any(), any(), any(), any());
    }

    @Test
    void checkAndDispatch_emptySpecIds_dispatchesNothing() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of()); // Here empty is intended

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verify(journalClient, never()).getHomeworks(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(settingsRepository, never()).save(any());
        verify(outboxEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void checkAndDispatch_unauthorizedError_disablesAutoHomework() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L)); // Fix: Added specId=5L

        when(journalClient.getHomeworks(anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(HttpClientErrorException.Unauthorized.create(org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        service.checkAndDispatch(settings);

        assertThat(settings.isEnabled()).isFalse();
        verify(settingsRepository).save(settings);
    }

    @Test
    void checkAndDispatch_journalClientReturnsNull_returnsEmptyStream() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L));

        when(journalClient.getHomeworks(anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(null);

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verify(outboxEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void checkAndDispatch_illegalStateError_doesNotDisableAutoHomework() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L));

        when(journalClient.getHomeworks(anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("Some internal error"));

        service.checkAndDispatch(settings);

        assertThat(settings.isEnabled()).isTrue();
        verify(settingsRepository, never()).save(settings);
    }

    @Test
    void checkAndDispatch_homeworkUrlBuiltFromFilePath() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L)); // Fix: Added specId=5L

        // filePath is relative — should be prefixed with journalUrl
        JournalHomeworkResponse hw = makeHomework(1, 5, 3, 10, "/files/hw.pdf");
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.NOT_COMPLETED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(hw));
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.EXPIRED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(homeworkExecutionRepository.existsByUserAndHomeworkId(user, 1L)).thenReturn(false);
        when(journalApiProperties.journalUrl()).thenReturn("https://journal.example.com");
        when(homeworkExecutionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            HomeworkExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(kafkaProperties.homeworkExecutionTopic()).thenReturn("topic");

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verify(homeworkExecutionRepository).saveAndFlush(argThat(ex ->
                "https://journal.example.com/files/hw.pdf".equals(ex.getHomeworkUrl())
        ));
    }

    @Test
    void checkAndDispatch_homeworkUrlAlreadyAbsolute_notPrefixed() {
        JournalGroup group = new JournalGroup(null, 10L, "Group A");
        User user = buildUserWithJournalUser(Set.of(group));
        UserAutoHomeworkSettings settings = buildSettings(user, true, Set.of(5L)); // Fix: Added specId=5L

        JournalHomeworkResponse hw = makeHomework(1, 5, 3, 10, "https://cdn.example.com/hw.pdf");
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.NOT_COMPLETED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(hw));
        when(journalClient.getHomeworks(anyInt(), eq(JournalHomeworkStatus.EXPIRED.getId()), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(homeworkExecutionRepository.existsByUserAndHomeworkId(user, 1L)).thenReturn(false);
        when(homeworkExecutionRepository.saveAndFlush(any())).thenAnswer(inv -> {
            HomeworkExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(kafkaProperties.homeworkExecutionTopic()).thenReturn("topic");

        ScopedValue.where(TelegramUserContext.TG_USER_ID, TELEGRAM_ID)
                .call(() -> {
                    service.checkAndDispatch(settings);
                    return null;
                });

        verify(homeworkExecutionRepository).saveAndFlush(argThat(ex ->
                "https://cdn.example.com/hw.pdf".equals(ex.getHomeworkUrl())
        ));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setTelegramId(TELEGRAM_ID);
        return u;
    }

    private User buildUserWithJournalUser(Set<JournalGroup> groups) {
        User u = buildUser();
        JournalUser ju = new JournalUser();
        ju.setJournalGroups(groups);
        u.setJournalUser(ju);
        return u;
    }

    private UserAutoHomeworkSettings buildSettings(User user, boolean enabled, Set<Long> specIds) {
        return UserAutoHomeworkSettings.builder()
                .user(user)
                .enabled(enabled)
                .specIds(new HashSet<>(specIds))
                .build();
    }

    private JournalHomeworkResponse makeHomework(int id, int specId, int teachId, int groupId, String filePath) {
        return new JournalHomeworkResponse(id, specId, teachId, groupId,
                "Teacher", "Math homework", null, null, null,
                "hw.pdf", filePath, "comment", "Mathematics",
                3, null, null, null, null);
    }
}
