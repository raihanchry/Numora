package com.example.ui;

import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.example.R;
import com.example.engine.AgeCalculatorEngine;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Modular Java controller powering the Age Calculator screen inside MainActivity,
 * featuring a fast 3-wheel vertical scroll picker (Day, Month, Year) instead of a calendar grid.
 */
public final class AgeCalculatorController {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    private final Context context;
    private final View rootView;

    private final TextView tvDobDate;
    private final TextView tvTargetDate;
    private final TextView tvAgeError;
    private final View ageResultsContainer;
    private final TextView tvAgeYears;
    private final TextView tvAgeMonths;
    private final TextView tvAgeDays;
    private final TextView tvBornWeekday;
    private final TextView tvNextBirthdayCountdown;
    private final TextView tvNextBirthdayWeekday;
    private final TextView tvTotalMonths;
    private final TextView tvTotalWeeks;
    private final TextView tvTotalDays;
    private final TextView tvTotalHours;
    private final TextView tvTotalMinutes;

    private int birthYear = 2000;
    private int birthMonth = 1;
    private int birthDay = 1;

    private int targetYear;
    private int targetMonth;
    private int targetDay;

    public AgeCalculatorController(
            @NonNull Context context,
            @NonNull FragmentManager fragmentManager,
            @NonNull View rootView
    ) {
        this.context = context;
        this.rootView = rootView;

        this.tvDobDate = rootView.findViewById(R.id.tvDobDate);
        this.tvTargetDate = rootView.findViewById(R.id.tvTargetDate);
        this.tvAgeError = rootView.findViewById(R.id.tvAgeError);
        this.ageResultsContainer = rootView.findViewById(R.id.ageResultsContainer);
        this.tvAgeYears = rootView.findViewById(R.id.tvAgeYears);
        this.tvAgeMonths = rootView.findViewById(R.id.tvAgeMonths);
        this.tvAgeDays = rootView.findViewById(R.id.tvAgeDays);
        this.tvBornWeekday = rootView.findViewById(R.id.tvBornWeekday);
        this.tvNextBirthdayCountdown = rootView.findViewById(R.id.tvNextBirthdayCountdown);
        this.tvNextBirthdayWeekday = rootView.findViewById(R.id.tvNextBirthdayWeekday);
        this.tvTotalMonths = rootView.findViewById(R.id.tvTotalMonths);
        this.tvTotalWeeks = rootView.findViewById(R.id.tvTotalWeeks);
        this.tvTotalDays = rootView.findViewById(R.id.tvTotalDays);
        this.tvTotalHours = rootView.findViewById(R.id.tvTotalHours);
        this.tvTotalMinutes = rootView.findViewById(R.id.tvTotalMinutes);

        resetTargetToToday();
        bindControls();
        renderAge();
    }

    private void resetTargetToToday() {
        Calendar today = Calendar.getInstance();
        targetYear = today.get(Calendar.YEAR);
        targetMonth = today.get(Calendar.MONTH) + 1;
        targetDay = today.get(Calendar.DAY_OF_MONTH);
    }

    private void bindControls() {
        rootView.findViewById(R.id.cardPickDob).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            openWheelDatePicker(true);
        });

        rootView.findViewById(R.id.cardPickTargetDate).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            openWheelDatePicker(false);
        });

        rootView.findViewById(R.id.btnResetToday).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            resetTargetToToday();
            renderAge();
        });
    }

    private void openWheelDatePicker(boolean pickingDob) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_wheel_date_picker, null, false);
        dialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvWheelPickerTitle);
        TextView tvPreview = sheetView.findViewById(R.id.tvWheelLivePreview);
        NumberPicker npDay = sheetView.findViewById(R.id.npDay);
        NumberPicker npMonth = sheetView.findViewById(R.id.npMonth);
        NumberPicker npYear = sheetView.findViewById(R.id.npYear);

        tvTitle.setText(pickingDob ? R.string.age_dob_label : R.string.age_target_label);

        int initYear = pickingDob ? birthYear : targetYear;
        int initMonth = pickingDob ? birthMonth : targetMonth;
        int initDay = pickingDob ? birthDay : targetDay;

        // Configure Year Wheel (1900..2100)
        npYear.setMinValue(MIN_YEAR);
        npYear.setMaxValue(MAX_YEAR);
        npYear.setValue(Math.max(MIN_YEAR, Math.min(MAX_YEAR, initYear)));
        npYear.setWrapSelectorWheel(false);

        // Configure Month Wheel (Jan..Dec)
        String[] shortMonths = buildShortMonthLabels();
        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setDisplayedValues(shortMonths);
        npMonth.setValue(initMonth);
        npMonth.setWrapSelectorWheel(true);

        // Configure Day Wheel (1..maxDaysInMonth with zero-padded display)
        npDay.setMinValue(1);
        npDay.setFormatter(value -> String.format(Locale.US, "%02d", value));
        int initialMaxDay = daysInMonth(npYear.getValue(), npMonth.getValue());
        npDay.setMaxValue(initialMaxDay);
        npDay.setValue(Math.min(initDay, initialMaxDay));
        npDay.setWrapSelectorWheel(true);

        Runnable updatePreview = () -> {
            int y = npYear.getValue();
            int m = npMonth.getValue();
            int maxDay = daysInMonth(y, m);
            if (npDay.getMaxValue() != maxDay) {
                int currentDay = npDay.getValue();
                npDay.setMaxValue(maxDay);
                if (currentDay > maxDay) {
                    npDay.setValue(maxDay);
                }
            }
            int d = npDay.getValue();
            tvPreview.setText(formatPreviewWithWeekday(y, m, d));
        };

        NumberPicker.OnValueChangeListener wheelListener = (picker, oldVal, newVal) -> {
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            updatePreview.run();
        };

        npDay.setOnValueChangedListener(wheelListener);
        npMonth.setOnValueChangedListener(wheelListener);
        npYear.setOnValueChangedListener(wheelListener);
        updatePreview.run();

        sheetView.findViewById(R.id.btnWheelCancel).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            dialog.dismiss();
        });

        sheetView.findViewById(R.id.btnWheelDone).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            int y = npYear.getValue();
            int m = npMonth.getValue();
            int d = Math.min(npDay.getValue(), daysInMonth(y, m));
            if (pickingDob) {
                birthYear = y;
                birthMonth = m;
                birthDay = d;
            } else {
                targetYear = y;
                targetMonth = m;
                targetDay = d;
            }
            renderAge();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void renderAge() {
        tvDobDate.setText(formatDate(birthYear, birthMonth, birthDay));
        tvTargetDate.setText(formatDate(targetYear, targetMonth, targetDay));

        AgeCalculatorEngine.AgeResult result = AgeCalculatorEngine.calculate(
                birthYear, birthMonth, birthDay,
                targetYear, targetMonth, targetDay
        );

        if (!result.isValid()) {
            tvAgeError.setVisibility(View.VISIBLE);
            ageResultsContainer.setVisibility(View.GONE);
            return;
        }

        tvAgeError.setVisibility(View.GONE);
        ageResultsContainer.setVisibility(View.VISIBLE);

        tvAgeYears.setText(String.valueOf(result.getYears()));
        tvAgeMonths.setText(String.valueOf(result.getMonths()));
        tvAgeDays.setText(String.valueOf(result.getDays()));
        tvBornWeekday.setText(context.getString(R.string.age_born_on, result.getBirthDayOfWeek()));

        if (result.isBirthdayToday()) {
            tvNextBirthdayCountdown.setText(R.string.age_birthday_today);
        } else if (result.getNextBirthdayMonths() > 0) {
            tvNextBirthdayCountdown.setText(context.getString(
                    R.string.age_birthday_countdown,
                    result.getNextBirthdayMonths(),
                    result.getNextBirthdayDays()
            ));
        } else {
            tvNextBirthdayCountdown.setText(context.getString(
                    R.string.age_birthday_days_only,
                    result.getNextBirthdayDays()
            ));
        }

        tvNextBirthdayWeekday.setText(context.getString(
                R.string.age_birthday_weekday,
                result.getNextBirthdayDayOfWeek()
        ));

        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        tvTotalMonths.setText(nf.format(result.getTotalMonths()));
        tvTotalWeeks.setText(nf.format(result.getTotalWeeks()));
        tvTotalDays.setText(nf.format(result.getTotalDays()));
        tvTotalHours.setText(nf.format(result.getTotalHours()));
        tvTotalMinutes.setText(nf.format(result.getTotalMinutes()));
    }

    @NonNull
    private String[] buildShortMonthLabels() {
        String[] raw = DateFormatSymbols.getInstance(Locale.getDefault()).getShortMonths();
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            months[i] = (i < raw.length && raw[i] != null && !raw[i].isEmpty())
                    ? raw[i]
                    : String.valueOf(i + 1);
        }
        return months;
    }

    private int daysInMonth(int year, int monthOneBased) {
        Calendar cal = AgeCalculatorEngine.createUtcDate(year, monthOneBased, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    @NonNull
    private String formatDate(int year, int monthOneBased, int day) {
        Calendar cal = AgeCalculatorEngine.createUtcDate(year, monthOneBased, day);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }

    @NonNull
    private String formatPreviewWithWeekday(int year, int monthOneBased, int day) {
        Calendar cal = AgeCalculatorEngine.createUtcDate(year, monthOneBased, day);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • EEEE", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }
}
