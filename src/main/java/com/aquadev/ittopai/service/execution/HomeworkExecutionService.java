package com.aquadev.ittopai.service.execution;

import java.util.UUID;

public interface HomeworkExecutionService {

    void complete(UUID id, String s3Key);
}
