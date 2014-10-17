package com.rosterloh.moodring.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Utilities related to app preferences
 *
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 17/10/2014
 */
public class PrefUtils {
    /**
     * Integer preference that indicates the number of times the ring should pulse [1-10]
     */
    public static final String PREF_PULSE_COUNT = "pref_pulse_count";

    /**
     * Integer preference indicates the loop delay in milliseconds for the pulse routine [1-100]
     */
    public static final String PREF_CYCLE_DELAY = "pref_cycle_delay";

    /**
     * Integer preference that indicates the intensity the ring should settle at after pulses complete [0-100%]
     */
    public static final String PREF_FINAL_INTENSITY = "pref_final_intensity";

    /**
     * Integer preference that indicates time in seconds for the ring to stay on after the colour is set [0-60s]
     */
    public static final String PREF_TIME_ON = "pref_time_on";

    public static long getPulseCount(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_PULSE_COUNT, 2L);
    }

    public static void setPulseCount(final Context context, long count) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_PULSE_COUNT, count).apply();
    }

    public static long getCycleDelay(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CYCLE_DELAY, 10L);
    }

    public static void setCycleDelay(final Context context, long delay) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CYCLE_DELAY, delay).apply();
    }

    public static long getFinalIntensity(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_FINAL_INTENSITY, 50L);
    }

    public static void setFinalIntensity(final Context context, long intensity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_FINAL_INTENSITY, intensity).apply();
    }

    public static long getTimeOn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_TIME_ON, 3L);
    }

    public static void setTimeOn(final Context context, long time) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_TIME_ON, time).apply();
    }

    public static void registerOnSharedPreferenceChangeListener(final Context context,
                                                                SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unrgisterOnSharedPreferenceChangeListener(final Context context,
                                                                 SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
