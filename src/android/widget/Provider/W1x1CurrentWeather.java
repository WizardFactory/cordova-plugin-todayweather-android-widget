package net.wizardfactory.todayweather.widget.Provider;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.util.TypedValue;
import android.widget.RemoteViews;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WeatherData;
import net.wizardfactory.todayweather.widget.Data.WidgetData;
import net.wizardfactory.todayweather.widget.JsonElement.WeatherElement;
import net.wizardfactory.todayweather.widget.SettingsActivity;

/**
 * Implementation of App Widget functionality.
 */
public class W1x1CurrentWeather extends TwWidgetProvider {

    public W1x1CurrentWeather() {
        TAG = "W1x1 CurrentWeather";
        mLayoutId = R.layout.w1x1_current_weather;
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);

        if (Build.MANUFACTURER.equals("samsung")) {
            if (Build.VERSION.SDK_INT >= 16) {
                views.setTextViewTextSize(R.id.location, TypedValue.COMPLEX_UNIT_DIP, 14);
                views.setTextViewTextSize(R.id.current_pm, TypedValue.COMPLEX_UNIT_DIP, 20);
                views.setTextViewTextSize(R.id.current_temperature, TypedValue.COMPLEX_UNIT_DIP, 20);
            }
        }

        int fontColor = SettingsActivity.loadFontColorPref(context, appWidgetId);
        views.setTextColor(R.id.location, fontColor);
        views.setTextColor(R.id.current_temperature, fontColor);

        TwWidgetProvider.setPendingIntentToMenu(context, appWidgetId, views);
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData, Units localUnits) {
        if (wData == null) {
            Log.e(TAG, "weather data is NULL");
            return;
        }

        if (wData.getLoc() != null) {
            views.setTextViewText(R.id.location, wData.getLoc());
        }

        WeatherData currentData = wData.getCurrentWeather();
        if (currentData == null) {
            Log.e(TAG, "currentElement is NULL");
            return;
        }

        views.setTextViewText(R.id.current_temperature, String.valueOf(currentData.getTemperature())+"°");

        int skyResourceId = context.getResources().getIdentifier(currentData.getSkyImageName(), "drawable", context.getPackageName());
        if (skyResourceId == -1) {
            skyResourceId = R.drawable.sun;
        }
        views.setImageViewResource(R.id.current_sky, skyResourceId);

        int pm10Grade = currentData.getPm10Grade();
        int pm25Grade = currentData.getPm25Grade();
        int color = -1;

        if (pm10Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
            views.setTextViewText(R.id.current_pm, ":::");
            if (pm25Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL && pm25Grade > pm10Grade) {
                color = getColorAqiGrade(context, pm25Grade, localUnits.getAirUnit());
                if (color != -1) {
                    views.setTextColor(R.id.current_pm, color);
                }
            }
            else {
                color = getColorAqiGrade(context, pm10Grade, localUnits.getAirUnit());
                if (color != -1) {
                    views.setTextColor(R.id.current_pm, color);
                }
            }
        }
        else if (pm25Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
            views.setTextViewText(R.id.current_pm, ":::");
            color = getColorAqiGrade(context, pm25Grade, localUnits.getAirUnit());
            if (color != -1) {
                views.setTextColor(R.id.current_pm, color);
            }
        }
    }

    static private int getColorAirKoreaGrade(Context context, int grade) {
        switch (grade) {
            case 1:
                return ContextCompat.getColor(context, android.R.color.holo_blue_dark);
            case 2:
                return ContextCompat.getColor(context, android.R.color.holo_green_dark);
            case 3:
                return ContextCompat.getColor(context, android.R.color.holo_orange_dark);
            case 4:
                return ContextCompat.getColor(context, android.R.color.holo_red_dark);
            default:
                Log.e(TAG, "Fail to find airkorea color grade="+grade);
        }
        return -1;
    }

    static private int getColorAirNowGrade(Context context, int grade) {
        switch (grade) {
            case 1:
                return ContextCompat.getColor(context, android.R.color.holo_green_dark);
            case 2:
                return ContextCompat.getColor(context, android.R.color.holo_orange_light);
            case 3:
                return ContextCompat.getColor(context, android.R.color.holo_orange_dark);
            case 4:
                return ContextCompat.getColor(context, android.R.color.holo_red_dark);
            case 5:
                return ContextCompat.getColor(context, android.R.color.holo_purple);
            case 6:
                return Color.parseColor("#800000"); //maroon
            default:
                Log.e(TAG, "Fail to find airnow color grade="+grade);
        }
        return -1;
    }

    static private int getColorAqiGrade(Context context, int grade, String airUnit) {
        if (airUnit.equals("airkorea") || airUnit.equals("airkorea_who"))  {
            return getColorAirKoreaGrade(context, grade);
        }
        else {
            return getColorAirNowGrade(context, grade);
        }
    }
}

