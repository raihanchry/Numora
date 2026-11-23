package com.example.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.R;
import com.example.engine.NumberFormatter;
import com.example.engine.UnitConverterEngine;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Modular Java controller powering the Unit Converter screen inside MainActivity.
 */
public final class UnitConverterController {

    public interface CopyCallback {
        void onCopyText(@NonNull String text);
    }

    private static final int MAX_INPUT_LENGTH = 16;

    private final Context context;
    private final View rootView;
    private final CopyCallback copyCallback;

    private final LinearLayout categoryContainer;
    private final TextView tvFromUnitName;
    private final TextView tvToUnitName;
    private final TextView tvFromValue;
    private final TextView tvToValue;

    private UnitConverterEngine.Category currentCategory = UnitConverterEngine.Category.LENGTH;
    private int fromUnitIndex;
    private int toUnitIndex;
    private String rawInput = "1";
    private String formattedOutput = "";

    public UnitConverterController(
            @NonNull Context context,
            @NonNull View rootView,
            @NonNull CopyCallback copyCallback
    ) {
        this.context = context;
        this.rootView = rootView;
        this.copyCallback = copyCallback;

        this.categoryContainer = rootView.findViewById(R.id.unitCategoryContainer);
        this.tvFromUnitName = rootView.findViewById(R.id.tvFromUnitName);
        this.tvToUnitName = rootView.findViewById(R.id.tvToUnitName);
        this.tvFromValue = rootView.findViewById(R.id.tvFromValue);
        this.tvToValue = rootView.findViewById(R.id.tvToValue);

        this.fromUnitIndex = UnitConverterEngine.getDefaultFromIndex(currentCategory);
        this.toUnitIndex = UnitConverterEngine.getDefaultToIndex(currentCategory);

        buildCategoryChips();
        bindControls();
        renderConversion();
    }

    private void buildCategoryChips() {
        categoryContainer.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        int hPad = (int) (14 * density);
        int marginEnd = (int) (8 * density);
        int chipHeight = (int) (36 * density);

        for (UnitConverterEngine.Category cat : UnitConverterEngine.Category.values()) {
            MaterialButton chip = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    chipHeight
            );
            lp.setMarginEnd(marginEnd);
            chip.setLayoutParams(lp);
            chip.setInsetTop(0);
            chip.setInsetBottom(0);
            chip.setMinHeight(chipHeight);
            chip.setMinWidth(0);
            chip.setMinimumWidth(0);
            chip.setPadding(hPad, 0, hPad, 0);
            chip.setCornerRadius((int) (18 * density));
            chip.setAllCaps(false);
            chip.setTextSize(12.5f);
            chip.setTypeface(ResourcesCompat.getFont(context, R.font.plus_jakarta_sans));
            chip.setText(getCategoryLabelRes(cat));
            chip.setTag(cat);
            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                selectCategory(cat);
            });
            categoryContainer.addView(chip);
        }
        updateCategoryChipStyles();
    }

    private void updateCategoryChipStyles() {
        int activeBg = ContextCompat.getColor(context, R.color.key_clear_bg);
        int activeText = ContextCompat.getColor(context, R.color.key_clear_text);
        int inactiveBg = ContextCompat.getColor(context, R.color.badge_bg);
        int inactiveText = ContextCompat.getColor(context, R.color.badge_text);

        for (int i = 0; i < categoryContainer.getChildCount(); i++) {
            View child = categoryContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton btn = (MaterialButton) child;
                boolean selected = currentCategory.equals(btn.getTag());
                btn.setBackgroundTintList(ColorStateList.valueOf(selected ? activeBg : inactiveBg));
                btn.setTextColor(selected ? activeText : inactiveText);
            }
        }
    }

    private void selectCategory(@NonNull UnitConverterEngine.Category category) {
        if (this.currentCategory == category) {
            return;
        }
        this.currentCategory = category;
        this.fromUnitIndex = UnitConverterEngine.getDefaultFromIndex(category);
        this.toUnitIndex = UnitConverterEngine.getDefaultToIndex(category);
        if (category != UnitConverterEngine.Category.TEMPERATURE && rawInput.startsWith("-")) {
            rawInput = rawInput.substring(1);
            if (rawInput.isEmpty()) {
                rawInput = "0";
            }
        }
        updateCategoryChipStyles();
        renderConversion();
    }

    private void bindControls() {
        rootView.findViewById(R.id.btnSelectFromUnit).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            showUnitPicker(true);
        });

        rootView.findViewById(R.id.btnSelectToUnit).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            showUnitPicker(false);
        });

        rootView.findViewById(R.id.btnSwapUnits).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            int tmp = fromUnitIndex;
            fromUnitIndex = toUnitIndex;
            toUnitIndex = tmp;
            renderConversion();
        });

        View.OnClickListener copyAction = v -> {
            if (!formattedOutput.isEmpty()) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                List<UnitConverterEngine.UnitItem> units =
                        UnitConverterEngine.getUnitsForCategory(currentCategory);
                String symbol = units.get(toUnitIndex).getSymbol();
                copyCallback.onCopyText(formattedOutput + " " + symbol);
            }
        };
        tvToValue.setOnClickListener(copyAction);
        rootView.findViewById(R.id.btnConvCopy).setOnClickListener(copyAction);

        bindDigit(R.id.btnConv0, "0");
        bindDigit(R.id.btnConv1, "1");
        bindDigit(R.id.btnConv2, "2");
        bindDigit(R.id.btnConv3, "3");
        bindDigit(R.id.btnConv4, "4");
        bindDigit(R.id.btnConv5, "5");
        bindDigit(R.id.btnConv6, "6");
        bindDigit(R.id.btnConv7, "7");
        bindDigit(R.id.btnConv8, "8");
        bindDigit(R.id.btnConv9, "9");
        bindDigit(R.id.btnConv00, "00");

        rootView.findViewById(R.id.btnConvDecimal).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (!rawInput.contains(".") && rawInput.length() < MAX_INPUT_LENGTH) {
                rawInput += ".";
                renderConversion();
            }
        });

        rootView.findViewById(R.id.btnConvBackspace).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (rawInput.length() <= 1 || (rawInput.length() == 2 && rawInput.startsWith("-"))) {
                rawInput = "0";
            } else {
                rawInput = rawInput.substring(0, rawInput.length() - 1);
            }
            renderConversion();
        });

        rootView.findViewById(R.id.btnConvClear).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            rawInput = "0";
            renderConversion();
        });

        rootView.findViewById(R.id.btnConvNegate).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if ("0".equals(rawInput)) {
                return;
            }
            if (rawInput.startsWith("-")) {
                rawInput = rawInput.substring(1);
            } else {
                rawInput = "-" + rawInput;
            }
            renderConversion();
        });
    }

    private void bindDigit(int buttonId, @NonNull String digits) {
        rootView.findViewById(buttonId).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (rawInput.length() + digits.length() > MAX_INPUT_LENGTH) {
                return;
            }
            if ("0".equals(rawInput)) {
                rawInput = "00".equals(digits) ? "0" : digits;
            } else if ("-0".equals(rawInput)) {
                rawInput = "00".equals(digits) ? "-0" : "-" + digits;
            } else {
                rawInput += digits;
            }
            renderConversion();
        });
    }

    private void showUnitPicker(boolean selectingFrom) {
        List<UnitConverterEngine.UnitItem> units =
                UnitConverterEngine.getUnitsForCategory(currentCategory);
        String[] labels = new String[units.size()];
        for (int i = 0; i < units.size(); i++) {
            labels[i] = units.get(i).getDisplayLabel();
        }
        int checked = selectingFrom ? fromUnitIndex : toUnitIndex;

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.converter_select_unit)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    if (selectingFrom) {
                        fromUnitIndex = which;
                    } else {
                        toUnitIndex = which;
                    }
                    renderConversion();
                    dialog.dismiss();
                })
                .show();
    }

    private void renderConversion() {
        List<UnitConverterEngine.UnitItem> units =
                UnitConverterEngine.getUnitsForCategory(currentCategory);
        UnitConverterEngine.UnitItem fromUnit = units.get(fromUnitIndex);
        UnitConverterEngine.UnitItem toUnit = units.get(toUnitIndex);

        tvFromUnitName.setText(fromUnit.getDisplayLabel());
        tvToUnitName.setText(toUnit.getDisplayLabel());

        String displayInput = NumberFormatter.formatExpressionForDisplay(rawInput.replace("-", "−"));
        tvFromValue.setText(displayInput);

        double parsed;
        try {
            parsed = Double.parseDouble(rawInput);
        } catch (NumberFormatException e) {
            parsed = 0.0;
        }

        double converted = UnitConverterEngine.convert(parsed, currentCategory, fromUnit, toUnit);
        formattedOutput = NumberFormatter.formatForDisplay(converted);
        tvToValue.setText(formattedOutput);
    }

    private int getCategoryLabelRes(@NonNull UnitConverterEngine.Category category) {
        switch (category) {
            case LENGTH:
                return R.string.cat_length;
            case WEIGHT:
                return R.string.cat_weight;
            case TEMPERATURE:
                return R.string.cat_temperature;
            case AREA:
                return R.string.cat_area;
            case VOLUME:
                return R.string.cat_volume;
            case SPEED:
                return R.string.cat_speed;
            case TIME:
                return R.string.cat_time;
            case DATA:
                return R.string.cat_data;
            default:
                return R.string.cat_length;
        }
    }
}
