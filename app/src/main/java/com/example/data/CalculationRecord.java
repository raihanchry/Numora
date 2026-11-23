package com.example.data;

import androidx.annotation.NonNull;

import com.example.engine.AngleMode;

/**
 * Immutable model representing one completed calculation in the local history tape.
 */
public final class CalculationRecord {

    private final long id;
    @NonNull
    private final String expression;
    @NonNull
    private final String formattedResult;
    @NonNull
    private final String plainResult;
    @NonNull
    private final AngleMode angleMode;
    private final long timestampMillis;

    public CalculationRecord(
            long id,
            @NonNull String expression,
            @NonNull String formattedResult,
            @NonNull String plainResult,
            @NonNull AngleMode angleMode,
            long timestampMillis
    ) {
        this.id = id;
        this.expression = expression;
        this.formattedResult = formattedResult;
        this.plainResult = plainResult;
        this.angleMode = angleMode;
        this.timestampMillis = timestampMillis;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getExpression() {
        return expression;
    }

    @NonNull
    public String getFormattedResult() {
        return formattedResult;
    }

    @NonNull
    public String getPlainResult() {
        return plainResult;
    }

    @NonNull
    public AngleMode getAngleMode() {
        return angleMode;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
