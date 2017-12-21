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

import java.util.Calendar;

/**
 * Implementation of App Widget functionality.
 */
public class W4x1Current3Hourly extends ClockAndCurrentWeather {
    static int[] labelIds = {R.id.label_first, R.id.label_second, R.id.label_third};
    static int[] tempIds = {R.id.first_temperature, R.id.second_temperature, R.id.third_temperature};
    static int[] skyIds = {R.id.first_sky, R.id.second_sky, R.id.third_sky};

    public W4x1Current3Hourly() {
        TAG = "W4x1 CurrentHourly";
        mLayoutId = R.layout.w4x1_current_hourly;
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);
        TwWidgetProvider.setWidgetInfoStyle(context, appWidgetId, views);

        if (Build.MANUFACTURER.equals("samsung")) {
            if (Build.VERSION.SDK_INT >= 16) {
                views.setTextViewTextSize(R.id.tmn_tmx_pm_pp, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.current_temperature, TypedValue.COMPLEX_UNIT_DIP, 48);

                for (int i = 0; i < 3; i++) {
                    views.setTextViewTextSize(labelIds[i], TypedValue.COMPLEX_UNIT_DIP, 16);
                    views.setTextViewTextSize(tempIds[i], TypedValue.COMPLEX_UNIT_DIP, 18);
                }
            }
        }

        int fontColor = SettingsActivity.loadFontColorPref(context, appWidgetId);
        views.setTextColor(R.id.tmn_tmx_pm_pp, fontColor);
        views.setTextColor(R.id.current_temperature, fontColor);
        for (int i = 0; i < 3; i++) {
            views.setTextColor(labelIds[i], fontColor);
            views.setTextColor(tempIds[i], fontColor);
        }

        TwWidgetProvider.setPendingIntentToRefresh(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToSettings(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToApp(context, appWidgetId, views);
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData, Units localUnits) {
        TwWidgetProvider.setWidgetInfoData(context, views, wData);

        if (wData == null) {
            Log.e(TAG, "weather data is NULL");
            return;
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

        views.setTextViewText(R.id.tmn_tmx_pm_pp, makeTmnTmxPmPpStr(context, wData, localUnits));

        Calendar calendar = Calendar.getInstance();

        for (int i=0; i<3; i++) {
            WeatherData hourlyData = wData.getHourlyWeather(i);

            calendar.setTime(hourlyData.getDate());
            String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY))
                    + context.getString(R.string.hour);
            views.setTextViewText(labelIds[i], hour);
            views.setTextViewText(tempIds[i], String.valueOf((int)hourlyData.getTemperature())+"°");

            if (hourlyData.getSky() != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                skyResourceId = context.getResources().getIdentifier(hourlyData.getSkyImageName(), "drawable", context.getPackageName());
                if (skyResourceId == -1) {
                    skyResourceId = R.drawable.sun;
                }
                views.setImageViewResource(skyIds[i], skyResourceId);
            }
        }
    }
}

