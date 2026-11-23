package com.example.engine;

import androidx.annotation.NonNull;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure Java calendar-accurate age calculator supporting minSdk 24+ without desugaring.
 * Computes exact years/months/days, next birthday countdown, weekday names, and lifetime totals.
 */
public final class AgeCalculatorEngine {

    public static final class AgeResult {
        private final boolean valid;
        private final int years;
        private final int months;
        private final int days;
        private final int nextBirthdayMonths;
        private final int nextBirthdayDays;
        private final boolean isBirthdayToday;
        private final String birthDayOfWeek;
        private final String nextBirthdayDayOfWeek;
        private final long totalMonths;
        private final long totalWeeks;
        private final long totalDays;
        private final long totalHours;
        private final long totalMinutes;

        private AgeResult(
                boolean valid,
                int years,
                int months,
                int days,
                int nextBirthdayMonths,
                int nextBirthdayDays,
                boolean isBirthdayToday,
                @NonNull String birthDayOfWeek,
                @NonNull String nextBirthdayDayOfWeek,
                long totalMonths,
                long totalWeeks,
                long totalDays,
                long totalHours,
                long totalMinutes
        ) {
            this.valid = valid;
            this.years = years;
            this.months = months;
            this.days = days;
            this.nextBirthdayMonths = nextBirthdayMonths;
            this.nextBirthdayDays = nextBirthdayDays;
            this.isBirthdayToday = isBirthdayToday;
            this.birthDayOfWeek = birthDayOfWeek;
            this.nextBirthdayDayOfWeek = nextBirthdayDayOfWeek;
            this.totalMonths = totalMonths;
            this.totalWeeks = totalWeeks;
            this.totalDays = totalDays;
            this.totalHours = totalHours;
            this.totalMinutes = totalMinutes;
        }

        @NonNull
        public static AgeResult invalid() {
            return new AgeResult(false, 0, 0, 0, 0, 0, false, "", "", 0, 0, 0, 0, 0);
        }

        public boolean isValid() {
            return valid;
        }

        public int getYears() {
            return years;
        }

        public int getMonths() {
            return months;
        }

        public int getDays() {
            return days;
        }

        public int getNextBirthdayMonths() {
            return nextBirthdayMonths;
        }

        public int getNextBirthdayDays() {
            return nextBirthdayDays;
        }

        public boolean isBirthdayToday() {
            return isBirthdayToday;
        }

        @NonNull
        public String getBirthDayOfWeek() {
            return birthDayOfWeek;
        }

        @NonNull
        public String getNextBirthdayDayOfWeek() {
            return nextBirthdayDayOfWeek;
        }

        public long getTotalMonths() {
            return totalMonths;
        }

        public long getTotalWeeks() {
            return totalWeeks;
        }

        public long getTotalDays() {
            return totalDays;
        }

        public long getTotalHours() {
            return totalHours;
        }

        public long getTotalMinutes() {
            return totalMinutes;
        }
    }

    private AgeCalculatorEngine() {
    }

    /**
     * Calculates exact age between {@code birthYear/Month/Day} and {@code targetYear/Month/Day}.
     * Months are 1-indexed (1 = January .. 12 = December).
     */
    @NonNull
    public static AgeResult calculate(
            int birthYear, int birthMonth, int birthDay,
            int targetYear, int targetMonth, int targetDay
    ) {
        Calendar dob = createUtcDate(birthYear, birthMonth, birthDay);
        Calendar target = createUtcDate(targetYear, targetMonth, targetDay);

        if (dob.after(target)) {
            return AgeResult.invalid();
        }

        int years = targetYear - birthYear;
        int months = targetMonth - birthMonth;
        int days = targetDay - birthDay;

        if (days < 0) {
            months -= 1;
            int prevMonth = targetMonth - 1;
            int prevYear = targetYear;
            if (prevMonth < 1) {
                prevMonth = 12;
                prevYear -= 1;
            }
            days += daysInMonth(prevYear, prevMonth);
        }

        if (months < 0) {
            years -= 1;
            months += 12;
        }

        long diffMillis = target.getTimeInMillis() - dob.getTimeInMillis();
        long totalDays = diffMillis / (24L * 60L * 60L * 1000L);
        long totalWeeks = totalDays / 7L;
        long totalHours = totalDays * 24L;
        long totalMinutes = totalHours * 60L;
        long totalMonths = (long) years * 12L + months;

        String birthWeekday = getWeekdayName(dob.get(Calendar.DAY_OF_WEEK));

        // Compute next birthday relative to target date
        boolean isBirthdayToday = (birthMonth == targetMonth)
                && (clampDay(targetYear, birthMonth, birthDay) == targetDay);

        int nextBdayYear = targetYear;
        int clampedDayThisYear = clampDay(nextBdayYear, birthMonth, birthDay);
        Calendar nextBirthday = createUtcDate(nextBdayYear, birthMonth, clampedDayThisYear);

        if (!isBirthdayToday && !nextBirthday.after(target)) {
            nextBdayYear += 1;
            nextBirthday = createUtcDate(
                    nextBdayYear,
                    birthMonth,
                    clampDay(nextBdayYear, birthMonth, birthDay)
            );
        }

        int nbMonths = 0;
        int nbDays = 0;
        if (!isBirthdayToday) {
            int nYear = nextBirthday.get(Calendar.YEAR);
            int nMonth = nextBirthday.get(Calendar.MONTH) + 1;
            int nDay = nextBirthday.get(Calendar.DAY_OF_MONTH);

            nbMonths = (nYear - targetYear) * 12 + (nMonth - targetMonth);
            nbDays = nDay - targetDay;
            if (nbDays < 0) {
                nbMonths -= 1;
                int prevM = nMonth - 1;
                int prevY = nYear;
                if (prevM < 1) {
                    prevM = 12;
                    prevY -= 1;
                }
                nbDays += daysInMonth(prevY, prevM);
            }
        }

        String nextBirthdayWeekday = getWeekdayName(nextBirthday.get(Calendar.DAY_OF_WEEK));

        return new AgeResult(
                true,
                years,
                months,
                days,
                nbMonths,
                nbDays,
                isBirthdayToday,
                birthWeekday,
                nextBirthdayWeekday,
                totalMonths,
                totalWeeks,
                totalDays,
                totalHours,
                totalMinutes
        );
    }

    @NonNull
    public static Calendar createUtcDate(int year, int monthOneBased, int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthOneBased - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        return cal;
    }

    private static int clampDay(int year, int monthOneBased, int day) {
        return Math.min(day, daysInMonth(year, monthOneBased));
    }

    private static int daysInMonth(int year, int monthOneBased) {
        Calendar cal = createUtcDate(year, monthOneBased, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    @NonNull
    private static String getWeekdayName(int calendarDayOfWeek) {
        String[] weekdays = DateFormatSymbols.getInstance(Locale.getDefault()).getWeekdays();
        if (calendarDayOfWeek >= 0 && calendarDayOfWeek < weekdays.length) {
            return weekdays[calendarDayOfWeek];
        }
        return "";
    }
}
