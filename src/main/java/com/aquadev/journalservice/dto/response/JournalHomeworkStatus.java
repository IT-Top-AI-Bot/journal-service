package com.aquadev.journalservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum JournalHomeworkStatus {
    EXPIRED(0, "Просрочено"),
    CHECKED(1, "Проверено"),
    IN_PROGRESS(2, "На проверке"),
    NOT_COMPLETED(3, "Не выполнено"),
    CHECKED_FINAL(4, "Общее количество ДЗ"),
    DELETED_BY_TEACHER(5, "ДЗ удалено преподавателем"),
    UNKNOWN(-1, "Неизвестно");

    private static final Map<Integer, JournalHomeworkStatus> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(JournalHomeworkStatus::getId, Function.identity()));

    private final int id;
    private final String displayName;

    public static JournalHomeworkStatus fromId(Integer id) {
        if (id == null) {
            return UNKNOWN;
        }
        return BY_ID.getOrDefault(id, UNKNOWN);
    }
}
