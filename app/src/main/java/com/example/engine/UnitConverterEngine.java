package com.example.engine;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure Java unit conversion engine covering 8 real-world measurement categories
 * with exact base-unit scaling and affine temperature conversions.
 */
public final class UnitConverterEngine {

    public enum Category {
        LENGTH,
        WEIGHT,
        TEMPERATURE,
        AREA,
        VOLUME,
        SPEED,
        TIME,
        DATA
    }

    public static final class UnitItem {
        private final String id;
        private final String name;
        private final String symbol;
        private final double toBaseFactor;

        public UnitItem(
                @NonNull String id,
                @NonNull String name,
                @NonNull String symbol,
                double toBaseFactor
        ) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
            this.toBaseFactor = toBaseFactor;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @NonNull
        public String getSymbol() {
            return symbol;
        }

        public double getToBaseFactor() {
            return toBaseFactor;
        }

        @NonNull
        public String getDisplayLabel() {
            return name + " (" + symbol + ")";
        }
    }

    private static final List<UnitItem> LENGTH_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("mm", "Millimeter", "mm", 0.001),
            new UnitItem("cm", "Centimeter", "cm", 0.01),
            new UnitItem("m", "Meter", "m", 1.0),
            new UnitItem("km", "Kilometer", "km", 1000.0),
            new UnitItem("in", "Inch", "in", 0.0254),
            new UnitItem("ft", "Foot", "ft", 0.3048),
            new UnitItem("yd", "Yard", "yd", 0.9144),
            new UnitItem("mi", "Mile", "mi", 1609.344)
    ));

    private static final List<UnitItem> WEIGHT_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("mg", "Milligram", "mg", 0.000001),
            new UnitItem("g", "Gram", "g", 0.001),
            new UnitItem("kg", "Kilogram", "kg", 1.0),
            new UnitItem("t", "Metric Ton", "t", 1000.0),
            new UnitItem("oz", "Ounce", "oz", 0.028349523125),
            new UnitItem("lb", "Pound", "lb", 0.45359237)
    ));

    private static final List<UnitItem> TEMPERATURE_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("c", "Celsius", "°C", 1.0),
            new UnitItem("f", "Fahrenheit", "°F", 1.0),
            new UnitItem("k", "Kelvin", "K", 1.0)
    ));

    private static final List<UnitItem> AREA_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("cm2", "Square Centimeter", "cm²", 0.0001),
            new UnitItem("m2", "Square Meter", "m²", 1.0),
            new UnitItem("km2", "Square Kilometer", "km²", 1_000_000.0),
            new UnitItem("ft2", "Square Foot", "ft²", 0.09290304),
            new UnitItem("ac", "Acre", "ac", 4046.8564224),
            new UnitItem("ha", "Hectare", "ha", 10_000.0)
    ));

    private static final List<UnitItem> VOLUME_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("ml", "Milliliter", "mL", 0.001),
            new UnitItem("l", "Liter", "L", 1.0),
            new UnitItem("m3", "Cubic Meter", "m³", 1000.0),
            new UnitItem("tsp", "Teaspoon (US)", "tsp", 0.00492892159375),
            new UnitItem("tbsp", "Tablespoon (US)", "tbsp", 0.01478676478125),
            new UnitItem("floz", "Fluid Ounce (US)", "fl oz", 0.0295735295625),
            new UnitItem("cup", "Cup (US)", "cup", 0.2365882365),
            new UnitItem("gal", "Gallon (US)", "gal", 3.785411784)
    ));

    private static final List<UnitItem> SPEED_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("ms", "Meter / second", "m/s", 1.0),
            new UnitItem("kmh", "Kilometer / hour", "km/h", 1.0 / 3.6),
            new UnitItem("mph", "Mile / hour", "mph", 0.44704),
            new UnitItem("kn", "Knot", "kn", 0.5144444444444445)
    ));

    private static final List<UnitItem> TIME_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("ms", "Millisecond", "ms", 0.001),
            new UnitItem("s", "Second", "s", 1.0),
            new UnitItem("min", "Minute", "min", 60.0),
            new UnitItem("hr", "Hour", "hr", 3600.0),
            new UnitItem("d", "Day", "d", 86400.0),
            new UnitItem("wk", "Week", "wk", 604800.0)
    ));

    private static final List<UnitItem> DATA_UNITS = Collections.unmodifiableList(Arrays.asList(
            new UnitItem("b", "Byte", "B", 1.0),
            new UnitItem("kb", "Kilobyte", "KB", 1024.0),
            new UnitItem("mb", "Megabyte", "MB", 1024.0 * 1024.0),
            new UnitItem("gb", "Gigabyte", "GB", 1024.0 * 1024.0 * 1024.0),
            new UnitItem("tb", "Terabyte", "TB", 1024.0 * 1024.0 * 1024.0 * 1024.0)
    ));

    private UnitConverterEngine() {
    }

    @NonNull
    public static List<UnitItem> getUnitsForCategory(@NonNull Category category) {
        switch (category) {
            case LENGTH:
                return LENGTH_UNITS;
            case WEIGHT:
                return WEIGHT_UNITS;
            case TEMPERATURE:
                return TEMPERATURE_UNITS;
            case AREA:
                return AREA_UNITS;
            case VOLUME:
                return VOLUME_UNITS;
            case SPEED:
                return SPEED_UNITS;
            case TIME:
                return TIME_UNITS;
            case DATA:
                return DATA_UNITS;
            default:
                return LENGTH_UNITS;
        }
    }

    public static int getDefaultFromIndex(@NonNull Category category) {
        switch (category) {
            case LENGTH:
                return 2; // Meter
            case WEIGHT:
                return 2; // Kilogram
            case AREA:
                return 1; // Square Meter
            case VOLUME:
                return 1; // Liter
            case SPEED:
                return 1; // km/h
            case TIME:
                return 3; // Hour
            case DATA:
                return 3; // GB
            case TEMPERATURE:
            default:
                return 0; // Celsius
        }
    }

    public static int getDefaultToIndex(@NonNull Category category) {
        switch (category) {
            case LENGTH:
                return 5; // Foot
            case WEIGHT:
                return 5; // Pound
            case AREA:
                return 3; // Square Foot
            case VOLUME:
                return 7; // Gallon
            case SPEED:
                return 2; // mph
            case TIME:
                return 2; // Minute
            case DATA:
                return 2; // MB
            case TEMPERATURE:
            default:
                return 1; // Fahrenheit
        }
    }

    public static double convert(
            double value,
            @NonNull Category category,
            @NonNull UnitItem from,
            @NonNull UnitItem to
    ) {
        if (from.getId().equals(to.getId())) {
            return value;
        }
        if (category == Category.TEMPERATURE) {
            return convertTemperature(value, from.getId(), to.getId());
        }
        double inBase = value * from.getToBaseFactor();
        return inBase / to.getToBaseFactor();
    }

    private static double convertTemperature(double value, @NonNull String fromId, @NonNull String toId) {
        double celsius;
        switch (fromId) {
            case "f":
                celsius = (value - 32.0) * 5.0 / 9.0;
                break;
            case "k":
                celsius = value - 273.15;
                break;
            case "c":
            default:
                celsius = value;
                break;
        }

        switch (toId) {
            case "f":
                return (celsius * 9.0 / 5.0) + 32.0;
            case "k":
                return celsius + 273.15;
            case "c":
            default:
                return celsius;
        }
    }
}
