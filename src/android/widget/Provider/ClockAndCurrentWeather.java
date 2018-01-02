package net.wizardfactory.todayweather.widget.Provider;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WeatherData;
import net.wizardfactory.todayweather.widget.Data.WidgetData;
import net.wizardfactory.todayweather.widget.JsonElement.WeatherElement;
import net.wizardfactory.todayweather.widget.SettingsActivity;

import java.util.TimeZone;

/**
 * Implementation of App Widget functionality.
 */
public class ClockAndCurrentWeather extends TwWidgetProvider {

    public ClockAndCurrentWeather() {
        TAG = "W3x1 ClockAndCurrentWeather";
        mLayoutId = R.layout.clock_and_current_weather;
    }

    static public void setWidgetClockCurrentStyle(Context context, int appWidgetId, RemoteViews views) {
        if (Build.MANUFACTURER.equals("samsung")) {
            if (Build.VERSION.SDK_INT >= 16) {
                views.setTextViewTextSize(R.id.date, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_DIP, 46);
                views.setTextViewTextSize(R.id.am_pm, TypedValue.COMPLEX_UNIT_DIP, 14);
                views.setTextViewTextSize(R.id.tmn_tmx_pm_pp, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.current_temperature, TypedValue.COMPLEX_UNIT_DIP, 46);
            }
        }

        int fontColor = SettingsActivity.loadFontColorPref(context, appWidgetId);
        views.setTextColor(R.id.tmn_tmx_pm_pp, fontColor);
        views.setTextColor(R.id.current_temperature, fontColor);
        if (Build.VERSION.SDK_INT >= 17) {
            views.setTextColor(R.id.date, fontColor);
            views.setTextColor(R.id.time, fontColor);
            views.setTextColor(R.id.am_pm, fontColor);
        }
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);
        TwWidgetProvider.setWidgetInfoStyle(context, appWidgetId, views);
        setWidgetClockCurrentStyle(context, appWidgetId, views);

        TwWidgetProvider.setPendingIntentToRefresh(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToSettings(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToApp(context, appWidgetId, views);
    }

    static public void setWidgetClockCurrentData(Context context, RemoteViews views, WidgetData wData, Units localUnits) {
        if (wData == null) {
            Log.e(TAG, "weather data is NULL");
            return;
        }

        WeatherData currentData = wData.getCurrentWeather();
        if (currentData == null) {
            Log.e(TAG, "currentElement is NULL");
            return;
        }

        if (Build.VERSION.SDK_INT >= 17) {
            if (currentData.getTimeZoneOffsetMS() != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                String zoneIds[] = TimeZone.getAvailableIDs(currentData.getTimeZoneOffsetMS());
                if (zoneIds.length > 0) {
                    views.setString(R.id.time, "setTimeZone", zoneIds[0]);
                    views.setString(R.id.date, "setTimeZone", zoneIds[0]);
                    views.setString(R.id.am_pm, "setTimeZone", zoneIds[0]);
                } else {
                    Log.e(TAG, "Fail to find time zone ids");
                }
            }
        }

        views.setTextViewText(R.id.current_temperature, String.valueOf(currentData.getTemperature())+"°");

        int skyResourceId = context.getResources().getIdentifier(currentData.getSkyImageName(), "drawable", context.getPackageName());
        if (skyResourceId == -1) {
            skyResourceId = R.drawable.sun;
        }
        views.setImageViewResource(R.id.current_sky, skyResourceId);
        views.setTextViewText(R.id.tmn_tmx_pm_pp, makeTmnTmxPmPpStr(context, wData, localUnits));
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData, Units localUnits) {
        TwWidgetProvider.setWidgetInfoData(context, views, wData);
        setWidgetClockCurrentData(context, views, wData, localUnits);
    }

    static protected String makeTmnTmxPmPpStr(Context context, WidgetData wData, Units localUnits) {
        WeatherData data = wData.getCurrentWeather();

        int minTemperature = (int)data.getMinTemperature();
        int maxTemperature = (int)data.getMaxTemperature();
        double temp;
        String today_tmn_tmx_pm_pp = "";
        if (minTemperature != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL)  {
            today_tmn_tmx_pm_pp += String.valueOf(minTemperature)+"°";
        }
        if (maxTemperature != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL)  {
            today_tmn_tmx_pm_pp += "/";
            today_tmn_tmx_pm_pp += String.valueOf(maxTemperature)+"°";
        }

        double rn1 = data.getRn1();
        if (rn1 != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL && rn1 != 0 ) {
            today_tmn_tmx_pm_pp += " ";
            today_tmn_tmx_pm_pp += rn1+localUnits.getPrecipitationUnit();
        }
        else {
            int pm10Grade = data.getPm10Grade();
            int pm25Grade = data.getPm25Grade();
            if (pm10Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                today_tmn_tmx_pm_pp += " :::";
                if (pm25Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL && pm25Grade > pm10Grade) {
                    today_tmn_tmx_pm_pp += data.getPm25Str();
                } else {
                    today_tmn_tmx_pm_pp += data.getPm10Str();
                }
            } else if (pm25Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                today_tmn_tmx_pm_pp += " :::";
                today_tmn_tmx_pm_pp += data.getPm25Str();
            }
        }
        return today_tmn_tmx_pm_pp;
    }
}

