package com.jmfavreau.pictoparle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingFragment extends PreferenceFragmentCompat {
    private PictoParleActivity activity;
    private Preference dark_screen;
    private Preference board_detection;
    private Preference screen_width_mm;
    private Preference screen_height_mm;
    private Preference interval_covered;
    private Preference interval_uncovered;
    private Preference double_tap_timeout;
    private Preference tap_timeout;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);
        activity = (PictoParleActivity) getActivity();

        dark_screen = getPreferenceManager().findPreference("darkscreen");
        dark_screen.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setActiveDarkscreen(newValue.toString().equals("true"));
                return true;
            }
        });

        board_detection = getPreferenceManager().findPreference("board_detection");
        board_detection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setActiveBoardDetection(newValue.toString().equals("true"));
                return true;
            }
        });

        screen_width_mm = getPreferenceManager().findPreference("screen_width_mm");
        screen_width_mm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setScreenWidthMM(Float.valueOf(newValue.toString()));
                return true;
            }
        });

        screen_height_mm = getPreferenceManager().findPreference("screen_height_mm");
        screen_height_mm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setScreenHeightMM(Float.valueOf(newValue.toString()));
                return true;
            }
        });

        interval_covered = getPreferenceManager().findPreference("interval_covered");
        interval_covered.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setIntervalCovered(Integer.valueOf(newValue.toString()));
                return true;
            }
        });

        interval_uncovered = getPreferenceManager().findPreference("interval_uncovered");
        interval_uncovered.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setIntervalUncovered(Integer.valueOf(newValue.toString()));
                return true;
            }
        });

        double_tap_timeout = getPreferenceManager().findPreference("double_tap_timeout");
        double_tap_timeout.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setDoubleTapTimeout(Integer.valueOf(newValue.toString()));
                return true;
            }
        });

        tap_timeout = getPreferenceManager().findPreference("tap_timeout");
        tap_timeout.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setTapTimeout(Integer.valueOf(newValue.toString()));
                return true;
            }
        });
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.boardDetector.setInactive();
        activity.setScreenVisible(true);
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
