package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.engine.AgeCalculatorEngine;
import com.example.engine.UnitConverterEngine;

import org.junit.Test;

import java.util.List;

/**
 * Pure Java unit tests for UnitConverterEngine and AgeCalculatorEngine.
 */
public class ConverterAndAgeEngineTest {

    @Test
    public void temperatureConversionsAreExact() {
        List<UnitConverterEngine.UnitItem> units =
                UnitConverterEngine.getUnitsForCategory(UnitConverterEngine.Category.TEMPERATURE);
        UnitConverterEngine.UnitItem c = units.get(0);
        UnitConverterEngine.UnitItem f = units.get(1);
        UnitConverterEngine.UnitItem k = units.get(2);

        assertEquals(32.0, UnitConverterEngine.convert(0.0, UnitConverterEngine.Category.TEMPERATURE, c, f), 1e-9);
        assertEquals(212.0, UnitConverterEngine.convert(100.0, UnitConverterEngine.Category.TEMPERATURE, c, f), 1e-9);
        assertEquals(-40.0, UnitConverterEngine.convert(-40.0, UnitConverterEngine.Category.TEMPERATURE, c, f), 1e-9);
        assertEquals(273.15, UnitConverterEngine.convert(0.0, UnitConverterEngine.Category.TEMPERATURE, c, k), 1e-9);
    }

    @Test
    public void lengthAndWeightConversionsAreExact() {
        List<UnitConverterEngine.UnitItem> lengths =
                UnitConverterEngine.getUnitsForCategory(UnitConverterEngine.Category.LENGTH);
        UnitConverterEngine.UnitItem m = lengths.get(2);
        UnitConverterEngine.UnitItem km = lengths.get(3);
        assertEquals(1000.0, UnitConverterEngine.convert(1.0, UnitConverterEngine.Category.LENGTH, km, m), 1e-9);

        List<UnitConverterEngine.UnitItem> weights =
                UnitConverterEngine.getUnitsForCategory(UnitConverterEngine.Category.WEIGHT);
        UnitConverterEngine.UnitItem g = weights.get(1);
        UnitConverterEngine.UnitItem kg = weights.get(2);
        assertEquals(2500.0, UnitConverterEngine.convert(2.5, UnitConverterEngine.Category.WEIGHT, kg, g), 1e-9);
    }

    @Test
    public void ageCalculationAcrossLeapYearAndBirthdayToday() {
        AgeCalculatorEngine.AgeResult result = AgeCalculatorEngine.calculate(
                2000, 1, 15,
                2026, 11, 22
        );
        assertTrue(result.isValid());
        assertEquals(26, result.getYears());
        assertEquals(10, result.getMonths());
        assertEquals(7, result.getDays());
        assertFalse(result.isBirthdayToday());
        assertEquals(1, result.getNextBirthdayMonths());
        assertEquals(24, result.getNextBirthdayDays());

        AgeCalculatorEngine.AgeResult birthdayToday = AgeCalculatorEngine.calculate(
                1998, 11, 22,
                2026, 11, 22
        );
        assertTrue(birthdayToday.isValid());
        assertTrue(birthdayToday.isBirthdayToday());
        assertEquals(28, birthdayToday.getYears());
        assertEquals(0, birthdayToday.getMonths());
        assertEquals(0, birthdayToday.getDays());
    }

    @Test
    public void futureDateOfBirthIsRejected() {
        AgeCalculatorEngine.AgeResult invalid = AgeCalculatorEngine.calculate(
                2030, 5, 1,
                2026, 11, 22
        );
        assertFalse(invalid.isValid());
    }
}
