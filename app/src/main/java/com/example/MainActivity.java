package com.example;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.example.data.CalculationRecord;
import com.example.databinding.ActivityMainBinding;
import com.example.engine.AngleMode;
import com.example.engine.EvaluationResult;
import com.example.ui.AgeCalculatorController;
import com.example.ui.CalculatorUiState;
import com.example.ui.CalculatorViewModel;
import com.example.ui.HistoryBottomSheet;
import com.example.ui.InlineHistoryAdapter;
import com.example.ui.UnitConverterController;

/**
 * Main Activity for Numora — pure Java Scientific Calculator, Unit Converter, and Age Calculator.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_ACTIVE_TAB = "active_tab";
    private static final int TAB_CALCULATOR = 0;
    private static final int TAB_CONVERTER = 1;
    private static final int TAB_AGE = 2;

    private ActivityMainBinding binding;
    private CalculatorViewModel viewModel;
    private InlineHistoryAdapter inlineHistoryAdapter;
    private UnitConverterController unitConverterController;
    private AgeCalculatorController ageCalculatorController;

    private int activeTab = TAB_CALCULATOR;
    private boolean lastScientificVisibilityInitialized = false;
    private boolean lastScientificVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CalculatorViewModel.class);
        AppCompatDelegate.setDefaultNightMode(viewModel.getSavedThemeNightMode());

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            activeTab = savedInstanceState.getInt(STATE_ACTIVE_TAB, TAB_CALCULATOR);
        }

        applyWindowInsets();
        setupInlineHistoryTape();
        setupToolControllers();
        bindTopBarControls();
        bindScientificKeypad();
        bindMainKeypad();
        setupBackHandling();

        renderActiveTab(false);
        viewModel.getUiState().observe(this, this::renderState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_TAB, activeTab);
    }

    private void applyWindowInsets() {
        final int baseLeft = binding.rootContainer.getPaddingLeft();
        final int baseTop = binding.rootContainer.getPaddingTop();
        final int baseRight = binding.rootContainer.getPaddingRight();
        final int baseBottom = binding.rootContainer.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContainer, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            view.setPadding(
                    baseLeft + insets.left,
                    baseTop + insets.top,
                    baseRight + insets.right,
                    baseBottom + insets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupInlineHistoryTape() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvInlineHistory.setLayoutManager(layoutManager);

        inlineHistoryAdapter = new InlineHistoryAdapter(new InlineHistoryAdapter.OnInlineItemClickListener() {
            @Override
            public void onInlineItemClick(@NonNull CalculationRecord record) {
                performKeyHaptic(binding.rvInlineHistory);
                viewModel.onSelectHistoryRecord(record, false);
            }

            @Override
            public void onInlineItemLongClick(@NonNull CalculationRecord record) {
                performKeyHaptic(binding.rvInlineHistory);
                viewModel.onSelectHistoryRecord(record, true);
            }
        });
        binding.rvInlineHistory.setAdapter(inlineHistoryAdapter);
    }

    private void setupToolControllers() {
        unitConverterController = new UnitConverterController(
                this,
                binding.includeUnitConverter.getRoot(),
                this::copyToClipboard
        );
        ageCalculatorController = new AgeCalculatorController(
                this,
                getSupportFragmentManager(),
                binding.includeAgeCalculator.getRoot()
        );
    }

    private void setupBackHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activeTab != TAB_CALCULATOR) {
                    activeTab = TAB_CALCULATOR;
                    renderActiveTab(true);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void bindTopBarControls() {
        binding.btnToggleScientific.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (activeTab != TAB_CALCULATOR) {
                activeTab = TAB_CALCULATOR;
                renderActiveTab(true);
                viewModel.setScientificMode(true);
                return;
            }
            CalculatorUiState state = viewModel.getUiState().getValue();
            boolean currentlyScientific = state != null && state.isScientificMode();
            viewModel.setScientificMode(!currentlyScientific);
        });

        binding.btnTabCalculator.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (activeTab != TAB_CALCULATOR) {
                activeTab = TAB_CALCULATOR;
                renderActiveTab(true);
            }
        });

        binding.btnTabConverter.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (activeTab != TAB_CONVERTER) {
                activeTab = TAB_CONVERTER;
                renderActiveTab(true);
            }
        });

        binding.btnTabAge.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (activeTab != TAB_AGE) {
                activeTab = TAB_AGE;
                renderActiveTab(true);
            }
        });

        binding.chipAngleMode.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.toggleAngleMode();
        });

        binding.btnToggleTheme.setOnClickListener(v -> {
            performKeyHaptic(v);
            boolean currentlyNight = isNightModeActive();
            int nextMode = currentlyNight
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES;
            viewModel.saveThemeNightMode(nextMode);
            AppCompatDelegate.setDefaultNightMode(nextMode);
        });

        View.OnClickListener copyClickListener = v -> copyCurrentCalculatorValue(v, false);
        View.OnLongClickListener copyLongClickListener = v -> copyCurrentCalculatorValue(v, true);

        binding.btnCopyResult.setOnClickListener(copyClickListener);
        binding.tvPrimaryResult.setOnClickListener(copyClickListener);
        binding.displayArea.setOnLongClickListener(copyLongClickListener);
        binding.tvPrimaryResult.setOnLongClickListener(copyLongClickListener);
        binding.tvExpression.setOnLongClickListener(copyLongClickListener);
    }

    private boolean copyCurrentCalculatorValue(@NonNull View view, boolean isLongPress) {
        CalculatorUiState state = viewModel.getUiState().getValue();
        if (state == null || state.hasError()) {
            return false;
        }
        String valueToCopy = !state.getResultText().isEmpty()
                ? state.getResultText()
                : state.getDisplayExpression();
        if (valueToCopy.isEmpty()) {
            return false;
        }
        view.performHapticFeedback(
                isLongPress ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.KEYBOARD_TAP
        );
        copyToClipboard(valueToCopy);
        return true;
    }

    private void renderActiveTab(boolean animate) {
        if (animate) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(180);
            TransitionManager.beginDelayedTransition(binding.contentContainer, transition);
        }

        binding.calculatorWorkspace.setVisibility(
                activeTab == TAB_CALCULATOR ? View.VISIBLE : View.GONE
        );
        binding.includeUnitConverter.getRoot().setVisibility(
                activeTab == TAB_CONVERTER ? View.VISIBLE : View.GONE
        );
        binding.includeAgeCalculator.getRoot().setVisibility(
                activeTab == TAB_AGE ? View.VISIBLE : View.GONE
        );

        int activeColor = ContextCompat.getColor(this, R.color.text_accent);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);

        binding.btnTabCalculator.setImageTintList(ColorStateList.valueOf(
                activeTab == TAB_CALCULATOR ? activeColor : inactiveColor
        ));
        binding.btnTabConverter.setImageTintList(ColorStateList.valueOf(
                activeTab == TAB_CONVERTER ? activeColor : inactiveColor
        ));
        binding.btnTabAge.setImageTintList(ColorStateList.valueOf(
                activeTab == TAB_AGE ? activeColor : inactiveColor
        ));

        CalculatorUiState state = viewModel.getUiState().getValue();
        boolean sciActive = activeTab == TAB_CALCULATOR && state != null && state.isScientificMode();
        binding.btnToggleScientific.setImageTintList(ColorStateList.valueOf(
                sciActive ? activeColor : inactiveColor
        ));
    }

    private void bindScientificKeypad() {
        binding.btnInv.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.toggleInverseMode();
        });

        binding.btnSin.setOnClickListener(v -> {
            performKeyHaptic(v);
            boolean inv = isInverseActive();
            viewModel.onFunction(inv ? "sin⁻¹(" : "sin(");
        });

        binding.btnCos.setOnClickListener(v -> {
            performKeyHaptic(v);
            boolean inv = isInverseActive();
            viewModel.onFunction(inv ? "cos⁻¹(" : "cos(");
        });

        binding.btnTan.setOnClickListener(v -> {
            performKeyHaptic(v);
            boolean inv = isInverseActive();
            viewModel.onFunction(inv ? "tan⁻¹(" : "tan(");
        });

        binding.btnPower.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (isInverseActive()) {
                viewModel.onSquare();
            } else {
                viewModel.onPower();
            }
        });

        binding.btnLog.setOnClickListener(v -> {
            performKeyHaptic(v);
            boolean inv = isInverseActive();
            viewModel.onFunction(inv ? "10^(" : "log(");
        });

        binding.btnLn.setOnClickListener(v -> {
            performKeyHaptic(v);
            boolean inv = isInverseActive();
            viewModel.onFunction(inv ? "e^(" : "ln(");
        });

        binding.btnPi.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (isInverseActive()) {
                viewModel.onNegate();
            } else {
                viewModel.onConstant("π");
            }
        });

        binding.btnEuler.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (isInverseActive()) {
                viewModel.onAnswer();
            } else {
                viewModel.onConstant("e");
            }
        });

        binding.btnFactorial.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onFactorial();
        });
    }

    private void bindMainKeypad() {
        binding.btn0.setOnClickListener(v -> onDigitClick(v, "0"));
        binding.btn1.setOnClickListener(v -> onDigitClick(v, "1"));
        binding.btn2.setOnClickListener(v -> onDigitClick(v, "2"));
        binding.btn3.setOnClickListener(v -> onDigitClick(v, "3"));
        binding.btn4.setOnClickListener(v -> onDigitClick(v, "4"));
        binding.btn5.setOnClickListener(v -> onDigitClick(v, "5"));
        binding.btn6.setOnClickListener(v -> onDigitClick(v, "6"));
        binding.btn7.setOnClickListener(v -> onDigitClick(v, "7"));
        binding.btn8.setOnClickListener(v -> onDigitClick(v, "8"));
        binding.btn9.setOnClickListener(v -> onDigitClick(v, "9"));

        binding.btnDecimal.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onDecimal();
        });

        binding.btnClear.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onClearAll();
        });

        binding.btnOpenParen.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onOpenParen();
        });

        binding.btnCloseParen.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onCloseParen();
        });

        binding.btnSqrt.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onFunction("√(");
        });

        binding.btnPercent.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onPercent();
        });

        binding.btnBackspace.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onBackspace();
        });

        binding.btnBackspace.setOnLongClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onClearAll();
            return true;
        });

        binding.btnMultiply.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onBinaryOperator("×");
        });

        binding.btnDivide.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onBinaryOperator("÷");
        });

        binding.btnSubtract.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onBinaryOperator("−");
        });

        binding.btnAdd.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onBinaryOperator("+");
        });

        binding.btnHistory.setOnClickListener(v -> {
            performKeyHaptic(v);
            if (getSupportFragmentManager().findFragmentByTag(HistoryBottomSheet.TAG) == null) {
                new HistoryBottomSheet().show(getSupportFragmentManager(), HistoryBottomSheet.TAG);
            }
        });

        binding.btnEquals.setOnClickListener(v -> {
            performKeyHaptic(v);
            viewModel.onEquals();
        });
    }

    private void onDigitClick(@NonNull View view, @NonNull String digit) {
        performKeyHaptic(view);
        viewModel.onDigit(digit);
    }

    private boolean isInverseActive() {
        CalculatorUiState state = viewModel.getUiState().getValue();
        return state != null && state.isInverseMode();
    }

    private void renderState(@NonNull CalculatorUiState state) {
        renderScientificSection(state.isScientificMode(), state.isInverseMode(), state.getAngleMode());
        renderDisplayAndTape(state);
    }

    private void renderScientificSection(
            boolean scientific,
            boolean inverse,
            @NonNull AngleMode angleMode
    ) {
        if (lastScientificVisibilityInitialized && lastScientificVisible != scientific) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(190);
            TransitionManager.beginDelayedTransition(binding.calculatorWorkspace, transition);
        }
        lastScientificVisibilityInitialized = true;
        lastScientificVisible = scientific;

        binding.scientificKeypadContainer.setVisibility(scientific ? View.VISIBLE : View.GONE);

        int activeColor = ContextCompat.getColor(this, R.color.text_accent);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);
        binding.btnToggleScientific.setImageTintList(ColorStateList.valueOf(
                activeTab == TAB_CALCULATOR && scientific ? activeColor : inactiveColor
        ));

        int angleLabelRes = angleMode == AngleMode.RADIANS ? R.string.angle_rad : R.string.angle_deg;
        binding.chipAngleMode.setText(angleLabelRes);
        binding.chipAngleMode.setVisibility(scientific ? View.VISIBLE : View.GONE);

        binding.chipInverseBadge.setVisibility(scientific && inverse ? View.VISIBLE : View.GONE);

        int invBgColor = ContextCompat.getColor(
                this,
                inverse ? R.color.key_sci_active_bg : R.color.key_sci_bg
        );
        int invTextColor = ContextCompat.getColor(
                this,
                inverse ? R.color.key_sci_active_text : R.color.key_sci_text
        );
        binding.btnInv.setBackgroundTintList(ColorStateList.valueOf(invBgColor));
        binding.btnInv.setTextColor(invTextColor);

        if (inverse) {
            binding.btnSin.setText("sin⁻¹");
            binding.btnSin.setContentDescription(getString(R.string.cd_asin));
            binding.btnCos.setText("cos⁻¹");
            binding.btnCos.setContentDescription(getString(R.string.cd_acos));
            binding.btnTan.setText("tan⁻¹");
            binding.btnTan.setContentDescription(getString(R.string.cd_atan));
            binding.btnPower.setText("x²");
            binding.btnPower.setContentDescription(getString(R.string.cd_square));
            binding.btnLog.setText("10ˣ");
            binding.btnLog.setContentDescription(getString(R.string.cd_pow10));
            binding.btnLn.setText("eˣ");
            binding.btnLn.setContentDescription(getString(R.string.cd_exp));
            binding.btnPi.setText("±");
            binding.btnPi.setContentDescription(getString(R.string.cd_negate));
            binding.btnEuler.setText("ANS");
            binding.btnEuler.setContentDescription(getString(R.string.cd_ans));
        } else {
            binding.btnSin.setText("sin");
            binding.btnSin.setContentDescription(getString(R.string.cd_sin));
            binding.btnCos.setText("cos");
            binding.btnCos.setContentDescription(getString(R.string.cd_cos));
            binding.btnTan.setText("tan");
            binding.btnTan.setContentDescription(getString(R.string.cd_tan));
            binding.btnPower.setText("xʸ");
            binding.btnPower.setContentDescription(getString(R.string.cd_power));
            binding.btnLog.setText("log");
            binding.btnLog.setContentDescription(getString(R.string.cd_log));
            binding.btnLn.setText("ln");
            binding.btnLn.setContentDescription(getString(R.string.cd_ln));
            binding.btnPi.setText("π");
            binding.btnPi.setContentDescription(getString(R.string.cd_pi));
            binding.btnEuler.setText("e");
            binding.btnEuler.setContentDescription(getString(R.string.cd_euler));
        }
    }

    private void renderDisplayAndTape(@NonNull CalculatorUiState state) {
        boolean excludeNewest = state.isFinalResult() && !state.getPreviousExpression().isEmpty();
        inlineHistoryAdapter.submitNewestFirst(state.getHistory(), excludeNewest);
        if (inlineHistoryAdapter.getItemCount() > 0) {
            binding.rvInlineHistory.scrollToPosition(inlineHistoryAdapter.getItemCount() - 1);
        }

        int primaryColor = ContextCompat.getColor(this, R.color.text_primary);
        int secondaryColor = ContextCompat.getColor(this, R.color.text_secondary);
        int errorColor = ContextCompat.getColor(this, R.color.text_error);

        String displayExpr = state.getDisplayExpression();

        if (state.hasError() && state.getErrorType() != null) {
            binding.tvExpression.setText(displayExpr);
            binding.tvExpression.setTextColor(primaryColor);
            binding.tvPrimaryResult.setText(mapErrorMessage(state.getErrorType()));
            binding.tvPrimaryResult.setTextColor(errorColor);
        } else if (state.isFinalResult()) {
            binding.tvExpression.setText(state.getPreviousExpression());
            binding.tvExpression.setTextColor(primaryColor);
            String finalVal = !state.getResultText().isEmpty() ? state.getResultText() : displayExpr;
            binding.tvPrimaryResult.setText("= " + finalVal);
            binding.tvPrimaryResult.setTextColor(primaryColor);
        } else if (displayExpr.isEmpty()) {
            binding.tvExpression.setText("");
            binding.tvPrimaryResult.setText("0");
            binding.tvPrimaryResult.setTextColor(secondaryColor);
        } else if (!state.getResultText().isEmpty()) {
            binding.tvExpression.setText(displayExpr);
            binding.tvExpression.setTextColor(primaryColor);
            binding.tvPrimaryResult.setText("= " + state.getResultText());
            binding.tvPrimaryResult.setTextColor(primaryColor);
        } else {
            binding.tvExpression.setText("");
            binding.tvPrimaryResult.setText(displayExpr);
            binding.tvPrimaryResult.setTextColor(primaryColor);
        }

        binding.hsvExpression.post(() -> binding.hsvExpression.fullScroll(View.FOCUS_RIGHT));

        boolean canCopy = !state.hasError()
                && (!state.getResultText().isEmpty() || !displayExpr.isEmpty());
        binding.btnCopyResult.setVisibility(canCopy ? View.VISIBLE : View.INVISIBLE);
    }

    @NonNull
    private String mapErrorMessage(@NonNull EvaluationResult.ErrorType errorType) {
        switch (errorType) {
            case DIVIDE_BY_ZERO:
                return getString(R.string.error_divide_by_zero);
            case DOMAIN:
                return getString(R.string.error_domain);
            case OVERFLOW:
                return getString(R.string.error_overflow);
            case SYNTAX:
            default:
                return getString(R.string.error_syntax);
        }
    }

    private void copyToClipboard(@NonNull String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(
                    this,
                    getString(R.string.copied_to_clipboard, text),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private boolean isNightModeActive() {
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void performKeyHaptic(@NonNull View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }
}
