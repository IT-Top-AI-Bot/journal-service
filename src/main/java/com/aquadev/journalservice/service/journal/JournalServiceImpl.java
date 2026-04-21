package com.aquadev.journalservice.service.journal;

import com.aquadev.journalservice.client.journal.JournalHomeworkQueryClient;
import com.aquadev.journalservice.client.journal.JournalReferenceClient;
import com.aquadev.journalservice.client.journal.JournalUserInfoClient;
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
import com.aquadev.journalservice.service.execution.HomeworkExecutionEventPublisher;
import com.aquadev.journalservice.service.user.group.UserGroupService;
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

    private final JournalUserInfoClient journalUserInfoClient;
    private final JournalHomeworkQueryClient journalHomeworkQueryClient;
    private final JournalReferenceClient journalReferenceClient;
    private final UserRepository userRepository;
    private final UserGroupService userGroupService;
    private final HomeworkExecutionMapper homeworkExecutionMapper;
    private final HomeworkExecutionRepository homeworkExecutionRepository;
    private final HomeworkExecutionEventPublisher homeworkExecutionEventPublisher;

    @Override
    public JournalUserResponse getCurrentUser() {
        return journalUserInfoClient.getCurrentUser();
    }

    @Override
    public List<JournalCountHomeworkResponse> getCountHomework() {
        List<JournalCountHomeworkResponse> all = journalHomeworkQueryClient.getCountHomework(null, null);
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
        return journalHomeworkQueryClient.getHomeworks(page, status, type, groupIdToUse.intValue(), (Integer) null);
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
        homeworkExecutionEventPublisher.publishCreated(execution);
        return execution;
    }

    @Override
    public List<JournalScheduleResponse> getScheduleByDate(LocalDate date) {
        return journalReferenceClient.getScheduleByDate(date);
    }

    @Override
    @Cacheable(value = "groupSpecs", cacheManager = "dailyCache", key = "@userGroupServiceImpl.getCurrentGroupId()")
    public List<JournalSpecResponse> getGroupSpecs() {
        return journalReferenceClient.getGroupSpecs();
    }
}
