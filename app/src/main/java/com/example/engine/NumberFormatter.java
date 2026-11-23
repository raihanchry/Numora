package com.example.engine;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formats double results with clean significant-digit rounding to remove IEEE-754 noise,
 * grouping commas for readability, and automatic scientific notation for extreme magnitudes.
 */
public final class NumberFormatter {

    private static final int SIGNIFICANT_DIGITS = 12;
    private static final MathContext MATH_CONTEXT =
            new MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP);
    private static final double SCIENTIFIC_UPPER_THRESHOLD = 1e12;
    private static final double SCIENTIFIC_LOWER_THRESHOLD = 1e-8;

    private NumberFormatter() {
    }

    /**
     * Formats a number for prominent display (with grouping commas and clean minus sign).
     */
    @NonNull
    public static String formatForDisplay(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        if (value == 0.0) {
            return "0";
        }

        BigDecimal rounded = normalize(value);
        if (rounded.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        double abs = rounded.abs().doubleValue();
        if (abs >= SCIENTIFIC_UPPER_THRESHOLD || abs < SCIENTIFIC_LOWER_THRESHOLD) {
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
            DecimalFormat sciFormat = new DecimalFormat("0.#########E0", symbols);
            String formatted = sciFormat.format(rounded);
            return formatted.replace("-", "−");
        }

        String plain = rounded.toPlainString();
        boolean negative = plain.startsWith("-");
        if (negative) {
            plain = plain.substring(1);
        }

        int dotIndex = plain.indexOf('.');
        String intPart = dotIndex >= 0 ? plain.substring(0, dotIndex) : plain;
        String fracPart = dotIndex >= 0 ? plain.substring(dotIndex) : "";

        String groupedInt = addGroupingCommas(intPart);
        return (negative ? "−" : "") + groupedInt + fracPart;
    }

    /**
     * Formats a number as an expression-safe plain string (no commas, standard calculator minus).
     */
    @NonNull
    public static String formatPlain(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        if (value == 0.0) {
            return "0";
        }

        BigDecimal rounded = normalize(value);
        if (rounded.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        double abs = rounded.abs().doubleValue();
        if (abs >= SCIENTIFIC_UPPER_THRESHOLD || abs < SCIENTIFIC_LOWER_THRESHOLD) {
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
            DecimalFormat sciFormat = new DecimalFormat("0.#########E0", symbols);
            return sciFormat.format(rounded).replace("-", "−");
        }

        return rounded.toPlainString().replace("-", "−");
    }

    /**
     * Formats raw numeric tokens inside an expression string with grouping commas for display.
     */
    @NonNull
    public static String formatExpressionForDisplay(@NonNull String rawExpression) {
        if (rawExpression.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(rawExpression.length() + 8);
        int i = 0;
        int len = rawExpression.length();
        while (i < len) {
            char c = rawExpression.charAt(i);
            if (Character.isDigit(c)) {
                int start = i;
                while (i < len && Character.isDigit(rawExpression.charAt(i))) {
                    i++;
                }
                String intDigits = rawExpression.substring(start, i);
                boolean isAfterDot = start > 0 && rawExpression.charAt(start - 1) == '.';
                boolean isExponent = start > 0 && (rawExpression.charAt(start - 1) == 'E'
                        || (start > 1 && rawExpression.charAt(start - 2) == 'E'
                        && (rawExpression.charAt(start - 1) == '-' || rawExpression.charAt(start - 1) == '−' || rawExpression.charAt(start - 1) == '+')));
                if (isAfterDot || isExponent || intDigits.length() <= 3) {
                    out.append(intDigits);
                } else {
                    out.append(addGroupingCommas(intDigits));
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    @NonNull
    private static BigDecimal normalize(double value) {
        BigDecimal bd = new BigDecimal(value, MATH_CONTEXT);
        bd = bd.stripTrailingZeros();
        // Avoid scientific notation like 1E+2 for plain integers such as 100
        if (bd.scale() < 0) {
            bd = bd.setScale(0, RoundingMode.UNNECESSARY);
        }
        return bd;
    }

    @NonNull
    private static String addGroupingCommas(@NonNull String digits) {
        int len = digits.length();
        if (len <= 3) {
            return digits;
        }
        StringBuilder sb = new StringBuilder(len + len / 3);
        int firstGroup = len % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        sb.append(digits, 0, firstGroup);
        for (int i = firstGroup; i < len; i += 3) {
            sb.append(',').append(digits, i, i + 3);
        }
        return sb.toString();
    }
}
