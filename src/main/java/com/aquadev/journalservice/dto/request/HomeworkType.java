package com.aquadev.journalservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HomeworkType {
    HOMEWORK(0),
    LAB_WORK(1);

    private final int id;
}
