package com.example.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable result returned by {@link ExpressionEvaluator}.
 */
public final class EvaluationResult {

    public enum ErrorType {
        DIVIDE_BY_ZERO,
        DOMAIN,
        OVERFLOW,
        SYNTAX
    }

    private final boolean success;
    private final double value;
    @Nullable
    private final String formattedDisplay;
    @Nullable
    private final String plainValue;
    @Nullable
    private final ErrorType errorType;

    private EvaluationResult(
            boolean success,
            double value,
            @Nullable String formattedDisplay,
            @Nullable String plainValue,
            @Nullable ErrorType errorType
    ) {
        this.success = success;
        this.value = value;
        this.formattedDisplay = formattedDisplay;
        this.plainValue = plainValue;
        this.errorType = errorType;
    }

    @NonNull
    public static EvaluationResult success(double value) {
        return new EvaluationResult(
                true,
                value,
                NumberFormatter.formatForDisplay(value),
                NumberFormatter.formatPlain(value),
                null
        );
    }

    @NonNull
    public static EvaluationResult error(@NonNull ErrorType errorType) {
        return new EvaluationResult(false, Double.NaN, null, null, errorType);
    }

    public boolean isSuccess() {
        return success;
    }

    public double getValue() {
        return value;
    }

    @NonNull
    public String getFormattedDisplay() {
        return formattedDisplay != null ? formattedDisplay : "";
    }

    @NonNull
    public String getPlainValue() {
        return plainValue != null ? plainValue : "";
    }

    @Nullable
    public ErrorType getErrorType() {
        return errorType;
    }
}
