package com.example.engine;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure Java recursive-descent mathematical expression evaluator supporting:
 * - Basic arithmetic (+, −, ×, ÷), parentheses, percentage (%), unary negation, implicit multiplication
 * - Trigonometric & inverse trigonometric functions (sin, cos, tan, sin⁻¹, cos⁻¹, tan⁻¹) in DEG or RAD
 * - Base-10 logarithm (log) and natural logarithm (ln)
 * - Right-associative exponentiation (^) and square root (√)
 * - Constants (π, e) and factorial (!)
 */
public final class ExpressionEvaluator {

    private ExpressionEvaluator() {
    }

    private enum TokenType {
        NUMBER,
        CONSTANT,
        FUNCTION,
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,
        POWER,
        PERCENT,
        FACTORIAL,
        LPAREN,
        RPAREN
    }

    private static final class Token {
        final TokenType type;
        final String text;
        final double numberValue;

        Token(TokenType type, String text, double numberValue) {
            this.type = type;
            this.text = text;
            this.numberValue = numberValue;
        }
    }

    private static final class EvalException extends Exception {
        final EvaluationResult.ErrorType errorType;

        EvalException(EvaluationResult.ErrorType errorType) {
            this.errorType = errorType;
        }
    }

    /**
     * Evaluates an expression strictly (used when user taps '=').
     * Automatically closes unclosed trailing parentheses for convenience.
     */
    @NonNull
    public static EvaluationResult evaluate(@NonNull String rawExpression, @NonNull AngleMode angleMode) {
        String prepared = prepareExpression(rawExpression, false);
        if (prepared.isEmpty()) {
            return EvaluationResult.error(EvaluationResult.ErrorType.SYNTAX);
        }
        return parseAndEvaluate(prepared, angleMode);
    }

    /**
     * Evaluates a partially typed expression for live preview.
     * Trims trailing incomplete operators before auto-closing parentheses.
     */
    @NonNull
    public static EvaluationResult evaluatePreview(@NonNull String rawExpression, @NonNull AngleMode angleMode) {
        String prepared = prepareExpression(rawExpression, true);
        if (prepared.isEmpty()) {
            return EvaluationResult.error(EvaluationResult.ErrorType.SYNTAX);
        }
        return parseAndEvaluate(prepared, angleMode);
    }

    @NonNull
    private static String prepareExpression(@NonNull String raw, boolean forPreview) {
        String cleaned = raw.replace(",", "").replace(" ", "").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        if (forPreview) {
            // Strip dangling binary operators at the very end so preview remains responsive
            while (!cleaned.isEmpty()) {
                char last = cleaned.charAt(cleaned.length() - 1);
                if (last == '+' || last == '-' || last == '−'
                        || last == '*' || last == '×'
                        || last == '/' || last == '÷'
                        || last == '^' || last == '(') {
                    cleaned = cleaned.substring(0, cleaned.length() - 1);
                } else {
                    break;
                }
            }
            if (cleaned.isEmpty()) {
                return "";
            }
        }

        // Auto-balance any unclosed opening parentheses
        int openCount = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '(') {
                openCount++;
            } else if (c == ')') {
                openCount--;
            }
        }
        if (openCount > 0) {
            StringBuilder sb = new StringBuilder(cleaned.length() + openCount);
            sb.append(cleaned);
            for (int i = 0; i < openCount; i++) {
                sb.append(')');
            }
            cleaned = sb.toString();
        }
        return cleaned;
    }

    @NonNull
    private static EvaluationResult parseAndEvaluate(@NonNull String expression, @NonNull AngleMode angleMode) {
        try {
            List<Token> tokens = tokenize(expression);
            if (tokens.isEmpty()) {
                return EvaluationResult.error(EvaluationResult.ErrorType.SYNTAX);
            }
            Parser parser = new Parser(tokens, angleMode);
            double result = parser.parseExpression();
            if (parser.hasRemaining()) {
                return EvaluationResult.error(EvaluationResult.ErrorType.SYNTAX);
            }
            if (Double.isNaN(result)) {
                return EvaluationResult.error(EvaluationResult.ErrorType.DOMAIN);
            }
            if (Double.isInfinite(result)) {
                return EvaluationResult.error(EvaluationResult.ErrorType.OVERFLOW);
            }
            if (Math.abs(result) == 0.0) {
                result = 0.0;
            }
            return EvaluationResult.success(result);
        } catch (EvalException e) {
            return EvaluationResult.error(e.errorType);
        } catch (Exception e) {
            return EvaluationResult.error(EvaluationResult.ErrorType.SYNTAX);
        }
    }

    @NonNull
    private static List<Token> tokenize(@NonNull String expr) throws EvalException {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = expr.length();

        while (i < len) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            // Number (including decimal and scientific notation like 1.5E-4 or 1.5E−4)
            if (Character.isDigit(c) || c == '.') {
                int start = i;
                boolean hasDot = (c == '.');
                i++;
                while (i < len) {
                    char nc = expr.charAt(i);
                    if (Character.isDigit(nc)) {
                        i++;
                    } else if (nc == '.' && !hasDot) {
                        hasDot = true;
                        i++;
                    } else {
                        break;
                    }
                }
                // Check for uppercase scientific exponent 'E'
                if (i < len && expr.charAt(i) == 'E') {
                    int expStart = i;
                    i++;
                    if (i < len && (expr.charAt(i) == '+' || expr.charAt(i) == '-' || expr.charAt(i) == '−')) {
                        i++;
                    }
                    int expDigitsStart = i;
                    while (i < len && Character.isDigit(expr.charAt(i))) {
                        i++;
                    }
                    if (i == expDigitsStart) {
                        // Not a valid exponent, rewind
                        i = expStart;
                    }
                }
                String numStr = expr.substring(start, i).replace('−', '-');
                if (".".equals(numStr)) {
                    throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
                }
                try {
                    double val = Double.parseDouble(numStr);
                    tokens.add(new Token(TokenType.NUMBER, numStr, val));
                } catch (NumberFormatException e) {
                    throw new EvalException(EvaluationResult.ErrorType.OVERFLOW);
                }
                continue;
            }

            // Operators & symbols
            switch (c) {
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+", 0));
                    i++;
                    continue;
                case '-':
                case '−':
                    tokens.add(new Token(TokenType.MINUS, "−", 0));
                    i++;
                    continue;
                case '*':
                case '×':
                    tokens.add(new Token(TokenType.MULTIPLY, "×", 0));
                    i++;
                    continue;
                case '/':
                case '÷':
                    tokens.add(new Token(TokenType.DIVIDE, "÷", 0));
                    i++;
                    continue;
                case '^':
                    tokens.add(new Token(TokenType.POWER, "^", 0));
                    i++;
                    continue;
                case '%':
                    tokens.add(new Token(TokenType.PERCENT, "%", 0));
                    i++;
                    continue;
                case '!':
                    tokens.add(new Token(TokenType.FACTORIAL, "!", 0));
                    i++;
                    continue;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "(", 0));
                    i++;
                    continue;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")", 0));
                    i++;
                    continue;
                case 'π':
                    tokens.add(new Token(TokenType.CONSTANT, "π", Math.PI));
                    i++;
                    continue;
                case '√':
                    tokens.add(new Token(TokenType.FUNCTION, "sqrt", 0));
                    i++;
                    continue;
                default:
                    break;
            }

            // Identifiers: functions (sin, cos, tan, sin⁻¹, cos⁻¹, tan⁻¹, asin, acos, atan, log, ln, sqrt) or constants (e, pi)
            if (Character.isLetter(c)) {
                int start = i;
                while (i < len && (Character.isLetter(expr.charAt(i))
                        || expr.charAt(i) == '⁻'
                        || expr.charAt(i) == '¹')) {
                    i++;
                }
                String ident = expr.substring(start, i);
                String lower = ident.toLowerCase(Locale.US);

                if ("e".equals(ident)) {
                    tokens.add(new Token(TokenType.CONSTANT, "e", Math.E));
                } else if ("pi".equals(lower)) {
                    tokens.add(new Token(TokenType.CONSTANT, "π", Math.PI));
                } else if (isSupportedFunction(lower)) {
                    tokens.add(new Token(TokenType.FUNCTION, normalizeFunctionName(lower), 0));
                } else {
                    throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
                }
                continue;
            }

            throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
        }

        return tokens;
    }

    private static boolean isSupportedFunction(@NonNull String name) {
        switch (name) {
            case "sin":
            case "cos":
            case "tan":
            case "asin":
            case "acos":
            case "atan":
            case "sin⁻¹":
            case "cos⁻¹":
            case "tan⁻¹":
            case "log":
            case "ln":
            case "sqrt":
                return true;
            default:
                return false;
        }
    }

    @NonNull
    private static String normalizeFunctionName(@NonNull String name) {
        switch (name) {
            case "sin⁻¹":
                return "asin";
            case "cos⁻¹":
                return "acos";
            case "tan⁻¹":
                return "atan";
            default:
                return name;
        }
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final AngleMode angleMode;
        private int pos = 0;

        Parser(List<Token> tokens, AngleMode angleMode) {
            this.tokens = tokens;
            this.angleMode = angleMode;
        }

        boolean hasRemaining() {
            return pos < tokens.size();
        }

        private Token peek() {
            return pos < tokens.size() ? tokens.get(pos) : null;
        }

        private Token consume() {
            return tokens.get(pos++);
        }

        // expression := term (('+' | '−') term)*
        double parseExpression() throws EvalException {
            double left = parseTerm();
            while (hasRemaining()) {
                Token t = peek();
                if (t.type == TokenType.PLUS) {
                    consume();
                    left = checkFinite(left + parseTerm());
                } else if (t.type == TokenType.MINUS) {
                    consume();
                    left = checkFinite(left - parseTerm());
                } else {
                    break;
                }
            }
            return left;
        }

        // term := implicitMul (('×' | '÷') implicitMul)*
        double parseTerm() throws EvalException {
            double left = parseImplicitMul();
            while (hasRemaining()) {
                Token t = peek();
                if (t.type == TokenType.MULTIPLY) {
                    consume();
                    left = checkFinite(left * parseImplicitMul());
                } else if (t.type == TokenType.DIVIDE) {
                    consume();
                    double right = parseImplicitMul();
                    if (right == 0.0) {
                        throw new EvalException(EvaluationResult.ErrorType.DIVIDE_BY_ZERO);
                    }
                    left = checkFinite(left / right);
                } else {
                    break;
                }
            }
            return left;
        }

        // implicitMul := unary (implicitFactor)*
        // Supports 2π, 2(3+4), (2+1)(3+1), 2sin(30), 2√(9), 3e
        double parseImplicitMul() throws EvalException {
            double left = parseUnary();
            while (hasRemaining()) {
                Token next = peek();
                if (next.type == TokenType.LPAREN
                        || next.type == TokenType.CONSTANT
                        || next.type == TokenType.FUNCTION) {
                    left = checkFinite(left * parseUnary());
                } else {
                    break;
                }
            }
            return left;
        }

        // unary := ('+' | '−') unary | power
        double parseUnary() throws EvalException {
            if (hasRemaining()) {
                Token t = peek();
                if (t.type == TokenType.PLUS) {
                    consume();
                    return parseUnary();
                }
                if (t.type == TokenType.MINUS) {
                    consume();
                    return -parseUnary();
                }
            }
            return parsePower();
        }

        // power := postfix ('^' unary)?  (right-associative; allows 2^-3 and 2^3^2 = 512)
        double parsePower() throws EvalException {
            double base = parsePostfix();
            if (hasRemaining() && peek().type == TokenType.POWER) {
                consume();
                double exponent = parseUnary();
                if (base == 0.0 && exponent < 0.0) {
                    throw new EvalException(EvaluationResult.ErrorType.DIVIDE_BY_ZERO);
                }
                if (base < 0.0 && Math.floor(exponent) != exponent) {
                    throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                }
                double result = Math.pow(base, exponent);
                if (Double.isNaN(result)) {
                    throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                }
                return checkFinite(result);
            }
            return base;
        }

        // postfix := primary ('!' | '%')*
        double parsePostfix() throws EvalException {
            double value = parsePrimary();
            while (hasRemaining()) {
                Token t = peek();
                if (t.type == TokenType.PERCENT) {
                    consume();
                    value = checkFinite(value / 100.0);
                } else if (t.type == TokenType.FACTORIAL) {
                    consume();
                    value = computeFactorial(value);
                } else {
                    break;
                }
            }
            return value;
        }

        // primary := NUMBER | CONSTANT | FUNCTION '(' expression ')' | FUNCTION unary | '(' expression ')'
        double parsePrimary() throws EvalException {
            if (!hasRemaining()) {
                throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
            }
            Token t = peek();
            if (t.type == TokenType.NUMBER || t.type == TokenType.CONSTANT) {
                consume();
                return t.numberValue;
            }
            if (t.type == TokenType.LPAREN) {
                consume();
                double value = parseExpression();
                if (!hasRemaining() || peek().type != TokenType.RPAREN) {
                    throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
                }
                consume();
                return value;
            }
            if (t.type == TokenType.FUNCTION) {
                consume();
                double arg;
                if (hasRemaining() && peek().type == TokenType.LPAREN) {
                    consume();
                    arg = parseExpression();
                    if (!hasRemaining() || peek().type != TokenType.RPAREN) {
                        throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
                    }
                    consume();
                } else {
                    // Allow concise forms like √9 or sin30
                    arg = parseUnary();
                }
                return evaluateFunction(t.text, arg);
            }

            throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
        }

        private double evaluateFunction(String name, double arg) throws EvalException {
            switch (name) {
                case "sin":
                    return evalSin(arg);
                case "cos":
                    return evalCos(arg);
                case "tan":
                    return evalTan(arg);
                case "asin":
                    if (arg < -1.0 || arg > 1.0) {
                        throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                    }
                    return fromRadians(Math.asin(arg));
                case "acos":
                    if (arg < -1.0 || arg > 1.0) {
                        throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                    }
                    return fromRadians(Math.acos(arg));
                case "atan":
                    return fromRadians(Math.atan(arg));
                case "log":
                    if (arg <= 0.0) {
                        throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                    }
                    return checkFinite(Math.log10(arg));
                case "ln":
                    if (arg <= 0.0) {
                        throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                    }
                    return checkFinite(Math.log(arg));
                case "sqrt":
                    if (arg < 0.0) {
                        throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                    }
                    return checkFinite(Math.sqrt(arg));
                default:
                    throw new EvalException(EvaluationResult.ErrorType.SYNTAX);
            }
        }

        private double evalSin(double arg) {
            if (angleMode == AngleMode.DEGREES) {
                double mod = normalizeDegrees(arg);
                if (mod == 0.0 || mod == 180.0) {
                    return 0.0;
                }
                if (mod == 90.0) {
                    return 1.0;
                }
                if (mod == 270.0) {
                    return -1.0;
                }
                return Math.sin(Math.toRadians(arg));
            }
            return Math.sin(arg);
        }

        private double evalCos(double arg) {
            if (angleMode == AngleMode.DEGREES) {
                double mod = normalizeDegrees(arg);
                if (mod == 90.0 || mod == 270.0) {
                    return 0.0;
                }
                if (mod == 0.0) {
                    return 1.0;
                }
                if (mod == 180.0) {
                    return -1.0;
                }
                return Math.cos(Math.toRadians(arg));
            }
            return Math.cos(arg);
        }

        private double evalTan(double arg) throws EvalException {
            if (angleMode == AngleMode.DEGREES) {
                double mod = normalizeDegrees(arg);
                if (mod == 90.0 || mod == 270.0) {
                    throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
                }
                if (mod == 0.0 || mod == 180.0) {
                    return 0.0;
                }
                if (mod == 45.0 || mod == 225.0) {
                    return 1.0;
                }
                if (mod == 135.0 || mod == 315.0) {
                    return -1.0;
                }
                return checkFinite(Math.tan(Math.toRadians(arg)));
            }
            double cos = Math.cos(arg);
            if (Math.abs(cos) < 1e-15) {
                throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
            }
            return checkFinite(Math.tan(arg));
        }

        private double normalizeDegrees(double degrees) {
            double mod = degrees % 360.0;
            if (mod < 0.0) {
                mod += 360.0;
            }
            return mod;
        }

        private double fromRadians(double radians) {
            return angleMode == AngleMode.DEGREES ? Math.toDegrees(radians) : radians;
        }

        private double computeFactorial(double value) throws EvalException {
            if (value < 0.0 || Math.floor(value) != value) {
                throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
            }
            if (value > 170.0) {
                throw new EvalException(EvaluationResult.ErrorType.OVERFLOW);
            }
            int n = (int) value;
            double acc = 1.0;
            for (int k = 2; k <= n; k++) {
                acc *= k;
            }
            return checkFinite(acc);
        }

        private double checkFinite(double value) throws EvalException {
            if (Double.isNaN(value)) {
                throw new EvalException(EvaluationResult.ErrorType.DOMAIN);
            }
            if (Double.isInfinite(value)) {
                throw new EvalException(EvaluationResult.ErrorType.OVERFLOW);
            }
            return value;
        }
    }
}
