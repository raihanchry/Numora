package com.example.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.data.CalculationRecord;
import com.example.data.HistoryRepository;
import com.example.engine.AngleMode;
import com.example.engine.EvaluationResult;
import com.example.engine.ExpressionEvaluator;
import com.example.engine.NumberFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * MVVM ViewModel managing calculator expression editing, live evaluation,
 * basic/scientific mode toggle, DEG/RAD angle mode, 2ND/INV toggle, and local history.
 */
public final class CalculatorViewModel extends AndroidViewModel {

    private static final String[] FUNCTION_SUFFIXES = new String[]{
            "sin⁻¹(", "cos⁻¹(", "tan⁻¹(",
            "sin(", "cos(", "tan(",
            "log(", "ln(", "√(",
            "10^(", "e^("
    };

    private static final int MAX_EXPRESSION_LENGTH = 120;

    private final HistoryRepository repository;
    private final MutableLiveData<CalculatorUiState> uiState = new MutableLiveData<>();

    private String expression = "";
    private String previousExpression = "";
    private String lastAnswer;
    private boolean justEvaluated = false;
    private boolean scientificMode;
    private boolean inverseMode = false;
    private AngleMode angleMode;
    private EvaluationResult.ErrorType activeError = null;
    private List<CalculationRecord> history;

    public CalculatorViewModel(@NonNull Application application) {
        super(application);
        this.repository = new HistoryRepository(application);
        this.scientificMode = repository.isScientificModeEnabled();
        this.angleMode = repository.getAngleMode();
        this.lastAnswer = repository.getLastAnswer();
        this.history = repository.loadHistory();
        publishState();
    }

    @NonNull
    public LiveData<CalculatorUiState> getUiState() {
        return uiState;
    }

    public int getSavedThemeNightMode() {
        return repository.getThemeNightMode();
    }

    public void saveThemeNightMode(int nightMode) {
        repository.setThemeNightMode(nightMode);
    }

    public void setScientificMode(boolean enabled) {
        if (this.scientificMode == enabled) {
            return;
        }
        this.scientificMode = enabled;
        repository.setScientificModeEnabled(enabled);
        publishState();
    }

    public void toggleInverseMode() {
        this.inverseMode = !this.inverseMode;
        publishState();
    }

    public void toggleAngleMode() {
        this.angleMode = (this.angleMode == AngleMode.DEGREES)
                ? AngleMode.RADIANS
                : AngleMode.DEGREES;
        repository.setAngleMode(this.angleMode);
        activeError = null;
        publishState();
    }

    public void onDigit(@NonNull String digit) {
        activeError = null;
        if (justEvaluated) {
            expression = "";
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }

        // Prevent redundant leading zeros on a numeric token (e.g. "00")
        String trailingNumber = extractTrailingNumberToken(expression);
        if ("0".equals(trailingNumber)) {
            if ("0".equals(digit)) {
                return;
            }
            expression = expression.substring(0, expression.length() - 1) + digit;
        } else {
            expression += digit;
        }
        publishState();
    }

    public void onDecimal() {
        activeError = null;
        if (justEvaluated) {
            expression = "0.";
            previousExpression = "";
            justEvaluated = false;
            publishState();
            return;
        }
        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }

        String trailingNumber = extractTrailingNumberToken(expression);
        if (trailingNumber.contains(".")) {
            return;
        }
        if (trailingNumber.isEmpty()) {
            if (!expression.isEmpty() && isImplicitMultiplierEnd(expression.charAt(expression.length() - 1))) {
                expression += "×0.";
            } else {
                expression += "0.";
            }
        } else {
            expression += ".";
        }
        publishState();
    }

    public void onBinaryOperator(@NonNull String operator) {
        activeError = null;
        if (justEvaluated) {
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }

        if (expression.isEmpty()) {
            if ("−".equals(operator)) {
                expression = "−";
                publishState();
            }
            return;
        }

        char last = expression.charAt(expression.length() - 1);
        if (last == '(') {
            if ("−".equals(operator)) {
                expression += "−";
                publishState();
            }
            return;
        }

        if (isBinaryOperatorChar(last)) {
            // Allow negative sign after ×, ÷, or ^ (e.g. 2^−3 or 5×−2)
            if ("−".equals(operator) && (last == '×' || last == '÷' || last == '^')) {
                expression += "−";
            } else if (expression.length() >= 2
                    && last == '−'
                    && isBinaryOperatorChar(expression.charAt(expression.length() - 2))) {
                // Replace two-char operator like "×−" with the new operator
                expression = expression.substring(0, expression.length() - 2) + operator;
            } else if (expression.length() == 1 && last == '−') {
                // Don't replace sole unary minus with ×/÷/+
                if ("+".equals(operator)) {
                    expression = "";
                }
            } else {
                expression = expression.substring(0, expression.length() - 1) + operator;
            }
        } else {
            expression += operator;
        }
        publishState();
    }

    public void onPower() {
        onBinaryOperator("^");
    }

    public void onSquare() {
        activeError = null;
        if (justEvaluated) {
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.isEmpty() || expression.length() + 2 > MAX_EXPRESSION_LENGTH) {
            return;
        }
        char last = expression.charAt(expression.length() - 1);
        if (canAttachPostfixOrPower(last)) {
            expression += "^2";
            publishState();
        }
    }

    public void onPercent() {
        activeError = null;
        if (justEvaluated) {
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.isEmpty() || expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }
        char last = expression.charAt(expression.length() - 1);
        if (canAttachPostfixOrPower(last)) {
            expression += "%";
            publishState();
        }
    }

    public void onFactorial() {
        activeError = null;
        if (justEvaluated) {
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.isEmpty() || expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }
        char last = expression.charAt(expression.length() - 1);
        if (canAttachPostfixOrPower(last)) {
            expression += "!";
            publishState();
        }
    }

    public void onFunction(@NonNull String funcTokenWithParen) {
        activeError = null;
        if (justEvaluated) {
            expression = "";
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.length() + funcTokenWithParen.length() > MAX_EXPRESSION_LENGTH) {
            return;
        }
        expression += funcTokenWithParen;
        publishState();
    }

    public void onConstant(@NonNull String constant) {
        activeError = null;
        if (justEvaluated) {
            expression = "";
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.length() + constant.length() > MAX_EXPRESSION_LENGTH) {
            return;
        }
        expression += constant;
        publishState();
    }

    public void onAnswer() {
        activeError = null;
        if (justEvaluated) {
            expression = "";
            previousExpression = "";
            justEvaluated = false;
        }
        String token = lastAnswer != null && !lastAnswer.isEmpty() ? lastAnswer : "0";
        if (expression.length() + token.length() > MAX_EXPRESSION_LENGTH) {
            return;
        }
        if (!expression.isEmpty()) {
            char last = expression.charAt(expression.length() - 1);
            if (Character.isDigit(last) || last == '.' || last == ')' || last == 'π' || last == 'e') {
                expression += "×";
            }
        }
        expression += token;
        publishState();
    }

    public void onOpenParen() {
        activeError = null;
        if (justEvaluated) {
            expression = "";
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.length() >= MAX_EXPRESSION_LENGTH) {
            return;
        }
        expression += "(";
        publishState();
    }

    public void onCloseParen() {
        activeError = null;
        if (justEvaluated || expression.isEmpty()) {
            return;
        }
        if (countUnclosedParens(expression) <= 0) {
            return;
        }
        char last = expression.charAt(expression.length() - 1);
        if (last == '(' || isBinaryOperatorChar(last)) {
            return;
        }
        expression += ")";
        publishState();
    }

    public void onNegate() {
        activeError = null;
        if (justEvaluated) {
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.isEmpty()) {
            expression = "−";
            publishState();
            return;
        }
        if ("−".equals(expression)) {
            expression = "";
            publishState();
            return;
        }

        // Toggle simple leading minus when the entire expression is a single signed number
        if (expression.startsWith("−") && isPureUnsignedNumber(expression.substring(1))) {
            expression = expression.substring(1);
            publishState();
            return;
        }
        if (isPureUnsignedNumber(expression)) {
            expression = "−" + expression;
            publishState();
            return;
        }

        // If trailing segment is "(−<num>)" unwrap it back to "<num>"
        int len = expression.length();
        if (expression.endsWith(")")) {
            int openIdx = expression.lastIndexOf("(−");
            if (openIdx >= 0) {
                String inner = expression.substring(openIdx + 2, len - 1);
                if (isPureUnsignedNumber(inner)) {
                    expression = expression.substring(0, openIdx) + inner;
                    publishState();
                    return;
                }
            }
        }

        // Otherwise negate the trailing unsigned number by wrapping in "(−<num>)"
        String trailing = extractTrailingNumberToken(expression);
        if (!trailing.isEmpty()) {
            int start = len - trailing.length();
            if (start >= 2 && expression.charAt(start - 1) == '−' && expression.charAt(start - 2) == '(') {
                expression = expression.substring(0, start - 2) + trailing;
            } else {
                expression = expression.substring(0, start) + "(−" + trailing + ")";
            }
        } else {
            char last = expression.charAt(len - 1);
            if (last == '−') {
                expression = expression.substring(0, len - 1);
            } else if (last == '(' || isBinaryOperatorChar(last)) {
                expression += "−";
            }
        }
        publishState();
    }

    public void onBackspace() {
        activeError = null;
        if (justEvaluated) {
            // Allow editing the evaluated result directly
            previousExpression = "";
            justEvaluated = false;
        }
        if (expression.isEmpty()) {
            return;
        }

        for (String suffix : FUNCTION_SUFFIXES) {
            if (expression.endsWith(suffix)) {
                expression = expression.substring(0, expression.length() - suffix.length());
                publishState();
                return;
            }
        }
        expression = expression.substring(0, expression.length() - 1);
        publishState();
    }

    public void onClearAll() {
        expression = "";
        previousExpression = "";
        justEvaluated = false;
        activeError = null;
        publishState();
    }

    public void onEquals() {
        if (expression.isEmpty()) {
            return;
        }
        EvaluationResult result = ExpressionEvaluator.evaluate(expression, angleMode);
        if (result.isSuccess()) {
            String formattedExpr = NumberFormatter.formatExpressionForDisplay(expression);
            String formattedResult = result.getFormattedDisplay();
            String plainResult = result.getPlainValue();

            previousExpression = formattedExpr;
            expression = plainResult;
            lastAnswer = plainResult;
            repository.setLastAnswer(plainResult);
            justEvaluated = true;
            activeError = null;

            // Only save non-trivial expressions into history
            if (!formattedExpr.equals(formattedResult)) {
                CalculationRecord record = new CalculationRecord(
                        System.currentTimeMillis(),
                        formattedExpr,
                        formattedResult,
                        plainResult,
                        angleMode,
                        System.currentTimeMillis()
                );
                history = repository.addRecord(record);
            }
        } else {
            activeError = result.getErrorType();
            justEvaluated = false;
        }
        publishState();
    }

    public void onSelectHistoryRecord(@NonNull CalculationRecord record, boolean useExpression) {
        activeError = null;
        if (useExpression) {
            expression = record.getExpression().replace(",", "");
            previousExpression = "";
            justEvaluated = false;
        } else {
            if (justEvaluated || expression.isEmpty()) {
                expression = record.getPlainResult();
                previousExpression = "";
                justEvaluated = false;
            } else {
                char last = expression.charAt(expression.length() - 1);
                if (Character.isDigit(last) || last == '.' || last == ')' || last == 'π' || last == 'e') {
                    expression += "×" + record.getPlainResult();
                } else {
                    expression += record.getPlainResult();
                }
            }
        }
        publishState();
    }

    public void onClearHistory() {
        repository.clearHistory();
        history = new ArrayList<>();
        publishState();
    }

    private void publishState() {
        String displayExpr = NumberFormatter.formatExpressionForDisplay(expression);
        String resultText = "";
        String plainResult = "";

        if (activeError != null) {
            resultText = "";
            plainResult = "";
        } else if (justEvaluated) {
            EvaluationResult eval = ExpressionEvaluator.evaluate(expression, angleMode);
            if (eval.isSuccess()) {
                resultText = eval.getFormattedDisplay();
                plainResult = eval.getPlainValue();
            } else {
                resultText = displayExpr;
                plainResult = expression;
            }
        } else if (!expression.isEmpty() && shouldShowLivePreview(expression)) {
            EvaluationResult preview = ExpressionEvaluator.evaluatePreview(expression, angleMode);
            if (preview.isSuccess()) {
                resultText = preview.getFormattedDisplay();
                plainResult = preview.getPlainValue();
            }
        }

        uiState.setValue(new CalculatorUiState(
                expression,
                displayExpr,
                resultText,
                plainResult,
                previousExpression,
                justEvaluated,
                scientificMode,
                inverseMode,
                angleMode,
                activeError,
                history
        ));
    }

    private static boolean shouldShowLivePreview(@NonNull String expr) {
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (!Character.isDigit(c) && c != '.' && !(i == 0 && c == '−')) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String extractTrailingNumberToken(@NonNull String expr) {
        int i = expr.length() - 1;
        while (i >= 0) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                i--;
            } else {
                break;
            }
        }
        return expr.substring(i + 1);
    }

    private static boolean isPureUnsignedNumber(@NonNull String s) {
        if (s.isEmpty()) {
            return false;
        }
        boolean seenDot = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (seenDot) {
                    return false;
                }
                seenDot = true;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBinaryOperatorChar(char c) {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '÷' || c == '^';
    }

    private static boolean canAttachPostfixOrPower(char last) {
        return Character.isDigit(last) || last == ')' || last == 'π' || last == 'e' || last == '!' || last == '%';
    }

    private static boolean isImplicitMultiplierEnd(char last) {
        return last == ')' || last == 'π' || last == 'e' || last == '!' || last == '%';
    }

    private static int countUnclosedParens(@NonNull String expr) {
        int open = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') {
                open++;
            } else if (c == ')') {
                open--;
            }
        }
        return open;
    }
}
