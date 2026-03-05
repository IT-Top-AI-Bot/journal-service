package com.aquadev.journalservice.exception.domain.homeworkexecution;

import com.aquadev.journalservice.exception.base.NotFoundException;

public class HomeworkExecutionNotFoundException extends NotFoundException {

    private static final String DEFAULT_MESSAGE = "Homework execution not found";

    public HomeworkExecutionNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public HomeworkExecutionNotFoundException(String message) {
        super(message);
    }
}
