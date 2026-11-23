package com.example.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.engine.AngleMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Device-local repository backed by SharedPreferences for calculation history and user UI preferences.
 * Requires zero Android permissions and keeps APK footprint minimal.
 */
public final class HistoryRepository {

    private static final String PREFS_NAME = "calco_preferences";
    private static final String KEY_HISTORY_JSON = "calculation_history_json";
    private static final String KEY_SCIENTIFIC_MODE = "scientific_mode_enabled";
    private static final String KEY_ANGLE_MODE = "angle_mode";
    private static final String KEY_LAST_ANSWER = "last_answer";
    private static final String KEY_THEME_MODE = "theme_night_mode";
    private static final int MAX_HISTORY_ENTRIES = 100;

    private final SharedPreferences prefs;

    public HistoryRepository(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public List<CalculationRecord> loadHistory() {
        String rawJson = prefs.getString(KEY_HISTORY_JSON, null);
        if (rawJson == null || rawJson.isEmpty()) {
            return new ArrayList<>();
        }
        List<CalculationRecord> records = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                long id = obj.optLong("id", i + 1L);
                String expression = obj.optString("expression", "");
                String formattedResult = obj.optString("formattedResult", "");
                String plainResult = obj.optString("plainResult", formattedResult);
                String angleStr = obj.optString("angleMode", AngleMode.DEGREES.name());
                AngleMode angleMode = AngleMode.RADIANS.name().equals(angleStr)
                        ? AngleMode.RADIANS
                        : AngleMode.DEGREES;
                long ts = obj.optLong("timestamp", 0L);
                if (!expression.isEmpty() && !formattedResult.isEmpty()) {
                    records.add(new CalculationRecord(
                            id, expression, formattedResult, plainResult, angleMode, ts
                    ));
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return records;
    }

    @NonNull
    public List<CalculationRecord> addRecord(@NonNull CalculationRecord record) {
        List<CalculationRecord> current = loadHistory();
        current.add(0, record);
        if (current.size() > MAX_HISTORY_ENTRIES) {
            current = new ArrayList<>(current.subList(0, MAX_HISTORY_ENTRIES));
        }
        saveHistory(current);
        return Collections.unmodifiableList(current);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY_JSON).apply();
    }

    private void saveHistory(@NonNull List<CalculationRecord> records) {
        JSONArray array = new JSONArray();
        for (CalculationRecord r : records) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", r.getId());
                obj.put("expression", r.getExpression());
                obj.put("formattedResult", r.getFormattedResult());
                obj.put("plainResult", r.getPlainResult());
                obj.put("angleMode", r.getAngleMode().name());
                obj.put("timestamp", r.getTimestampMillis());
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_HISTORY_JSON, array.toString()).apply();
    }

    public boolean isScientificModeEnabled() {
        return prefs.getBoolean(KEY_SCIENTIFIC_MODE, false);
    }

    public void setScientificModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCIENTIFIC_MODE, enabled).apply();
    }

    @NonNull
    public AngleMode getAngleMode() {
        String stored = prefs.getString(KEY_ANGLE_MODE, AngleMode.DEGREES.name());
        return AngleMode.RADIANS.name().equals(stored) ? AngleMode.RADIANS : AngleMode.DEGREES;
    }

    public void setAngleMode(@NonNull AngleMode mode) {
        prefs.edit().putString(KEY_ANGLE_MODE, mode.name()).apply();
    }

    @NonNull
    public String getLastAnswer() {
        return prefs.getString(KEY_LAST_ANSWER, "0");
    }

    public void setLastAnswer(@NonNull String plainAnswer) {
        prefs.edit().putString(KEY_LAST_ANSWER, plainAnswer).apply();
    }

    public int getThemeNightMode() {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeNightMode(int nightMode) {
        prefs.edit().putInt(KEY_THEME_MODE, nightMode).apply();
    }
}
