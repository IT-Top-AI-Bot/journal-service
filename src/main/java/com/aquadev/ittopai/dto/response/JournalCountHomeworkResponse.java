package com.aquadev.ittopai.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record JournalCountHomeworkResponse(
        Integer counterTypeId,
        JournalHomeworkCounterType counterTypeName,
        Integer counter
) {

    @JsonCreator
    public JournalCountHomeworkResponse(
            @JsonProperty("counterType") @JsonAlias("counter_type") Integer counterType,
            @JsonProperty("counter") Integer counter
    ) {
        this(counterType, JournalHomeworkCounterType.fromId(counterType), counter);
    }
}
