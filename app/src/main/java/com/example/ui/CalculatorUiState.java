package com.example.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.data.CalculationRecord;
import com.example.engine.AngleMode;
import com.example.engine.EvaluationResult;

import java.util.Collections;
import java.util.List;

/**
 * Immutable UI state snapshot rendered by {@link com.example.MainActivity}.
 */
public final class CalculatorUiState {

    @NonNull
    private final String rawExpression;
    @NonNull
    private final String displayExpression;
    @NonNull
    private final String resultText;
    @NonNull
    private final String plainResult;
    @NonNull
    private final String previousExpression;
    private final boolean isFinalResult;
    private final boolean isScientificMode;
    private final boolean isInverseMode;
    @NonNull
    private final AngleMode angleMode;
    @Nullable
    private final EvaluationResult.ErrorType errorType;
    @NonNull
    private final List<CalculationRecord> history;

    public CalculatorUiState(
            @NonNull String rawExpression,
            @NonNull String displayExpression,
            @NonNull String resultText,
            @NonNull String plainResult,
            @NonNull String previousExpression,
            boolean isFinalResult,
            boolean isScientificMode,
            boolean isInverseMode,
            @NonNull AngleMode angleMode,
            @Nullable EvaluationResult.ErrorType errorType,
            @NonNull List<CalculationRecord> history
    ) {
        this.rawExpression = rawExpression;
        this.displayExpression = displayExpression;
        this.resultText = resultText;
        this.plainResult = plainResult;
        this.previousExpression = previousExpression;
        this.isFinalResult = isFinalResult;
        this.isScientificMode = isScientificMode;
        this.isInverseMode = isInverseMode;
        this.angleMode = angleMode;
        this.errorType = errorType;
        this.history = Collections.unmodifiableList(history);
    }

    @NonNull
    public String getRawExpression() {
        return rawExpression;
    }

    @NonNull
    public String getDisplayExpression() {
        return displayExpression;
    }

    @NonNull
    public String getResultText() {
        return resultText;
    }

    @NonNull
    public String getPlainResult() {
        return plainResult;
    }

    @NonNull
    public String getPreviousExpression() {
        return previousExpression;
    }

    public boolean isFinalResult() {
        return isFinalResult;
    }

    public boolean isScientificMode() {
        return isScientificMode;
    }

    public boolean isInverseMode() {
        return isInverseMode;
    }

    @NonNull
    public AngleMode getAngleMode() {
        return angleMode;
    }

    @Nullable
    public EvaluationResult.ErrorType getErrorType() {
        return errorType;
    }

    @NonNull
    public List<CalculationRecord> getHistory() {
        return history;
    }

    public boolean hasError() {
        return errorType != null;
    }
}
