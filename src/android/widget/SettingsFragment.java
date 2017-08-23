package net.wizardfactory.todayweather.widget;


import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import net.wizardfactory.todayweather.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link PreferenceFragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = "SettingsFragment";

    private SharedPreferences mSharedPreferences;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private boolean setCityList() {
        ListPreference location = (ListPreference)findPreference("location");
        JSONArray cityListArray = SettingsActivity.loadCityListPref(getActivity());
        if (cityListArray == null) {
           return false;
        }

        CharSequence[] locations = new CharSequence[cityListArray.length()];
        CharSequence[] locationValues = new CharSequence[cityListArray.length()];
        for (int i = 0; i < cityListArray.length(); i++) {
            try {
                JSONObject object = cityListArray.getJSONObject(i);
                boolean currentPosition = object.getBoolean("currentPosition");
                if (currentPosition) {
                    locations[i] = getActivity().getString(R.string.current_position);
                } else {
                    if (object.has("name")) {
                        locations[i] = object.get("name").toString();
                    } else {
                        locations[i] = object.get("address").toString();
                    }
                }
                locationValues[i] = object.toString();
            } catch (JSONException e) {
                Log.e(TAG, "JSONException: " + e.getMessage());
                continue;
            }
        }

        if (locations.length <= 0) {
           return false;
        }

        int defaultIndex = 0;
        String savedCityInfo = SettingsActivity.loadCityInfoPref(getActivity(), mAppWidgetId);
        Log.i("SettingsFragment", "saved city info="+savedCityInfo);

        for (int i=0; i<locationValues.length; i++) {
           if (locationValues[i].toString().equals(savedCityInfo))  {
               defaultIndex = i;
           }
        }

        location.setEntries(locations);
        location.setEntryValues(locationValues);
        location.setValueIndex(defaultIndex);
        location.setSummary(location.getEntry());

        location.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference location = (ListPreference) preference;
                int index = location.findIndexOfValue((String)newValue);
                CharSequence[] entries = location.getEntries();
                location.setSummary(entries[index]);
                return true;
            }
        });
        return true;
    }

    private boolean setRefreshIntervalList() {
        int defaultIndex = 2;
        long savedRefreshInterval = SettingsActivity.loadUpdateIntervalPref(getActivity(), mAppWidgetId);
        savedRefreshInterval /= 60000;
        Log.i("SettingsFragment", "update interval="+savedRefreshInterval);

        ListPreference refreshInterval = (ListPreference)findPreference("refreshInterval");
        CharSequence charSequence[] = refreshInterval.getEntryValues();
        for (int i=0; i<charSequence.length; i++) {
            if (savedRefreshInterval == Integer.parseInt(charSequence[i].toString())) {
                defaultIndex = i;
            }
        }

        refreshInterval.setValueIndex(defaultIndex);
        refreshInterval.setSummary(refreshInterval.getEntry());

        refreshInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference refreshInterval = (ListPreference) preference;
                int index = refreshInterval.findIndexOfValue((String) newValue);
                CharSequence[] entries = refreshInterval.getEntries();
                refreshInterval.setSummary(entries[index]);
                return true;
            }
        });

        return true;
    }

    private boolean setTransparencyList() {
        int defaultIndex = 0;
        int savedTransparency = SettingsActivity.loadTransparencyPref(getActivity(), mAppWidgetId);
        Log.i("SettingsFragment", "transparency="+savedTransparency);

        ListPreference transparency = (ListPreference)findPreference("transparency");

        CharSequence charSequence[] = transparency.getEntryValues();
         for (int i=0; i<charSequence.length; i++) {
            if (savedTransparency == Integer.parseInt(charSequence[i].toString())) {
                defaultIndex = i;
            }
        }

        transparency.setValueIndex(defaultIndex);
        if (defaultIndex == 0) {
            CharSequence[] entries = transparency.getEntries();
            transparency.setSummary(entries[defaultIndex]);
        } else {
            transparency.setSummary("%s");
        }

        transparency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference transparency = (ListPreference) preference;
                int index = transparency.findIndexOfValue((String) newValue);

                if (index == 0) {
                    CharSequence[] entries = transparency.getEntries();
                    transparency.setSummary(entries[index]);
                } else {
                    transparency.setSummary("%s");
                }
                return true;
            }
        });

        return true;
    }

    private boolean setBackgroundColorList() {
        int savedBgColor = SettingsActivity.loadBgColorPref(getActivity(), mAppWidgetId);
        Log.i("SettingsFragment", "background color="+savedBgColor);

        com.kizitonwose.colorpreference.ColorPreference backgroundColor = (com.kizitonwose.colorpreference.ColorPreference)findPreference("backgroundColor");
        backgroundColor.setValue(savedBgColor);
        backgroundColor.setSummary(String.format("#%06X", (0xFFFFFF & savedBgColor)));

        backgroundColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                com.kizitonwose.colorpreference.ColorPreference backgroundColor = (com.kizitonwose.colorpreference.ColorPreference) preference;
                int color = (Integer) newValue;
                backgroundColor.setSummary(String.format("#%06X", (0xFFFFFF & color)));
                return true;
            }
        });

        return true;
    }

    private boolean setFontColorList() {
        int savedFontColor = SettingsActivity.loadFontColorPref(getActivity(), mAppWidgetId);
        Log.i("SettingsFragment", "font color="+savedFontColor);
        com.kizitonwose.colorpreference.ColorPreference fontColor = (com.kizitonwose.colorpreference.ColorPreference)findPreference("fontColor");
        fontColor.setValue(savedFontColor);
        fontColor.setSummary(String.format("#%06X", (0xFFFFFF & savedFontColor)));

        fontColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                com.kizitonwose.colorpreference.ColorPreference fontColor = (com.kizitonwose.colorpreference.ColorPreference) preference;
                int color = (Integer) newValue;
                fontColor.setSummary(String.format("#%06X", (0xFFFFFF & color)));
                return true;
            }
        });
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.widget_preferences);

        if (setCityList() == false) {
            onShowAlertDialog();
            return;
        }
        if (setRefreshIntervalList() == false) {
            onShowAlertDialog();
            return;
        }
        if (setTransparencyList() == false) {
            onShowAlertDialog();
            return;
        }
        if (setBackgroundColorList() == false) {
            onShowAlertDialog();
            return;
        }
        if (setFontColorList() == false) {
            onShowAlertDialog();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    private void onShowAlertDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                getActivity().finish();
            }
        });
        alert.setMessage(R.string.add_locations);
        alert.show();
    }

    public void setAppWidgetId(int appWidgetId) {
        mAppWidgetId = appWidgetId;
    }
}
