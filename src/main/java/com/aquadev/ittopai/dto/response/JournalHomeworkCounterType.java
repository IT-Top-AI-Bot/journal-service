package com.aquadev.ittopai.dto.response;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum JournalHomeworkCounterType {
    EXPIRED(0, "Просрочено"),
    CHECKED(1, "Проверено"),
    NONE(2, "-"),
    IGNORED(3, "Игнорируется"),
    CHECKED_FINAL(4, "Проверено"),
    DELETED_BY_TEACHER(5, "ДЗ удалено преподавателем"),
    UNKNOWN(-1, "Неизвестно");

    private static final Map<Integer, JournalHomeworkCounterType> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(JournalHomeworkCounterType::getId, Function.identity()));

    private final int id;
    private final String displayName;

    JournalHomeworkCounterType(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static JournalHomeworkCounterType fromId(Integer id) {
        if (id == null) {
            return UNKNOWN;
        }
        return BY_ID.getOrDefault(id, UNKNOWN);
    }
}
