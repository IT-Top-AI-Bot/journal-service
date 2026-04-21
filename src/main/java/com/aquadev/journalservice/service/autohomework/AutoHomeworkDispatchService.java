package com.aquadev.journalservice.service.autohomework;

import com.aquadev.journalservice.model.UserAutoHomeworkSettings;

public interface AutoHomeworkDispatchService {

    void checkAndDispatch(UserAutoHomeworkSettings settings);
}
