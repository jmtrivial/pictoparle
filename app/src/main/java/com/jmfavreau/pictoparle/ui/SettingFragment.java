package com.jmfavreau.pictoparle.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;

public class SettingFragment extends PreferenceFragmentCompat {
    private PictoParleActivity activity;
    private Preference dark_screen;
    private Preference board_detection;
    private Preference screen_width_mm;
    private Preference screen_height_mm;
    private Preference interval_covered;
    private Preference interval_uncovered;
    private Preference distance_tap;
    private Preference distance_double_tap;
    private Preference double_tap_timeout;
    private Preference tap_timeout;
    private Preference audio_verbosity;
    private Preference device_model;
    private ArrayMap<String, Float> screenWidth;
    private ArrayMap<String, Float> screenHeight;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        screenWidth = new ArrayMap<>();
        screenHeight = new ArrayMap<>();
        screenWidth.put("lenovo-tab-e10", 216.0f);
        screenHeight.put("lenovo-tab-e10", 135.0f);
        screenWidth.put("lenovo-tab-m10", 217.0f);
        screenHeight.put("lenovo-tab-m10", 136.0f);
        screenWidth.put("samsung-galaxy-a3", 106.0f);
        screenHeight.put("samsung-galaxy-a3", 60.5f);
        screenWidth.put("samsung-tab-a7-sm-t500", 226.1f);
        screenHeight.put("samsung-tab-a7-sm-t500", 136.6f);
        screenWidth.put("custom", -1.0f);
        screenHeight.put("custom", -1.0f);

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
                float value = Float.valueOf(newValue.toString());
                activity.setScreenWidthMM(value);
                screenWidth.put("custom", value);
                return true;
            }
        });

        screen_height_mm = getPreferenceManager().findPreference("screen_height_mm");
        screen_height_mm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                float value = Float.valueOf(newValue.toString());
                activity.setScreenHeightMM(value);
                screenHeight.put("custom", value);
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

        distance_tap = getPreferenceManager().findPreference("tap_max_distance");
        distance_tap.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
           @Override
           public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setMaxTapDistance(Float.valueOf(newValue.toString()));
                return true;
           }
        });

        distance_double_tap = getPreferenceManager().findPreference("double_tap_max_distance");
        distance_double_tap.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setMaxDoubleTapDistance(Float.valueOf(newValue.toString()));
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

        audio_verbosity = getPreferenceManager().findPreference("audio_verbosity");
        audio_verbosity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                activity.setAudioVerbosity(Integer.valueOf(newValue.toString()));
                return true;
            }
        });

        device_model = getPreferenceManager().findPreference("device_model");
        device_model.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setScreenSizeSettings(newValue.toString());
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
        setTitle();
        activity.setCurrentFragment(null);
        setScreenSizeSettings();
        activity.findViewById(R.id.add_button).setVisibility(View.GONE);
    }


    private String deviceName() {
        if (device_model instanceof ListPreference) {
            ListPreference listPref = (ListPreference) device_model;
            return listPref.getValue();
        }
        else {
            return "custom";
        }
    }
    private void setScreenSizeSettings() {
        screenWidth.put("custom", Float.valueOf(((EditTextPreference) screen_width_mm).getText()));
        screenHeight.put("custom", Float.valueOf(((EditTextPreference) screen_height_mm).getText()));
        setScreenSizeSettings(deviceName());
    }

    private void setScreenSizeSettings(String value) {
        screen_width_mm.setVisible(value.equals("custom"));
        screen_height_mm.setVisible(value.equals("custom"));
        EditTextPreference editWPref = (EditTextPreference) screen_width_mm;
        editWPref.setText(String.valueOf(screenWidth.get(value)));
        EditTextPreference editHPref = (EditTextPreference) screen_height_mm;
        editHPref.setText(String.valueOf(screenHeight.get(value)));
        activity.setScreenWidthMM(screenWidth.get(value));
        activity.setScreenHeightMM(screenHeight.get(value));
    }

    public void setTitle() {
        activity.getSupportActionBar().setTitle("Préférences");
    }

}
