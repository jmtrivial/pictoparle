package com.jmfavreau.pictoparle.ui;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
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
import androidx.preference.PreferenceScreen;

import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;
import com.jmfavreau.pictoparle.core.Board;
import com.jmfavreau.pictoparle.core.Device;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

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
    private ArrayList<String> entries;
    private ArrayList<String> entryValues;

    private static final String CUSTOM = "custom";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preferences, rootKey);
        try {
            buildDeviceList(getContext().getPackageName());
        } catch (IOException e) {
            Log.e("PictoParle", "IO Error while reading a device description.");
        } catch (XmlPullParserException e) {
            Log.e("PictoParle", "XML Error while reading a device description.");
        }
        updateDeviceList();
    }

    private void updateDeviceList() {
        ListPreference prefList = findPreference("device_model");
        prefList.setEntries(entries.toArray(new String[0]));
        prefList.setEntryValues(entryValues.toArray(new String[0]));
        prefList.setDefaultValue(entryValues.get(0));
    }

    private void buildDeviceList(String packagename) throws XmlPullParserException, IOException {
        screenWidth = new ArrayMap<>();
        screenHeight = new ArrayMap<>();
        entries = new ArrayList<>();
        entryValues = new ArrayList<>();

        Resources resources = getResources();
        // load boards
        XmlResourceParser xrp = resources.getXml(R.xml.devices);
        do {

            if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                if (xrp.getName().equals("device")) {
                    int identifier = resources.getIdentifier(
                            xrp.getAttributeValue(null, "name"),
                            "xml", packagename);
                    XmlResourceParser parser = resources.getXml(identifier);
                    Device device = new Device(parser);
                    if (device.isValid()) {
                        addDevice(device);
                    }
                }
            }
        } while (xrp.next() != XmlResourceParser.END_DOCUMENT);

        Device custom = new Device();
        addDevice(custom);
    }

    private void addDevice(Device device) {
        String key = device.getDeviceKey();
        screenWidth.put(key, device.getScreenWidth());
        screenHeight.put(key, device.getScreenHeight());
        entries.add(device.getDeviceName());
        entryValues.add(key);
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
                screenHeight.put(CUSTOM, value);
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

        boolean popup = !activity.isScreenSizeDefined();

        activity.setScreenVisible(true);
        setTitle();
        activity.setCurrentFragment(null);
        setScreenSizeSettings();
        activity.findViewById(R.id.add_button).setVisibility(View.GONE);

        if (popup) {
            getPreferenceManager().showDialog(device_model);
        }
    }

    
    private String deviceName() {
        if (device_model instanceof ListPreference) {
            ListPreference listPref = (ListPreference) device_model;
            if (listPref.getValue() != null)
                return listPref.getValue();
            else
                return CUSTOM;
        }
        else {
            return CUSTOM;
        }
    }
    private void setScreenSizeSettings() {
        screenWidth.put(CUSTOM, Float.valueOf(((EditTextPreference) screen_width_mm).getText()));
        screenHeight.put(CUSTOM, Float.valueOf(((EditTextPreference) screen_height_mm).getText()));
        setScreenSizeSettings(deviceName());
    }

    private void setScreenSizeSettings(String value) {
        screen_width_mm.setVisible(value.equals(CUSTOM));
        screen_height_mm.setVisible(value.equals(CUSTOM));
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
