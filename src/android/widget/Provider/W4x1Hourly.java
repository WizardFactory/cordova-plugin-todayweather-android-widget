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
public class W4x1Hourly extends TwWidgetProvider {

    static public int[] labelIds = {R.id.label_now, R.id.label_first, R.id.label_second,
            R.id.label_third, R.id.label_fourth};
    static public int[] tempIds = {R.id.now_temperature, R.id.first_temperature,
            R.id.second_temperature, R.id.third_temperature, R.id.fourth_temperature};
    static public int[] skyIds = {R.id.now_sky, R.id.first_sky, R.id.second_sky, R.id.third_sky,
            R.id.fourth_sky};

    public W4x1Hourly() {
        TAG = "W4x1 Hourly";
        mLayoutId = R.layout.w4x1_hourly;

    }

    static public void setWidgetHourlyStyle(Context context, int appWidgetId, RemoteViews views) {
        if (Build.MANUFACTURER.equals("samsung")) {
            if (Build.VERSION.SDK_INT >= 16) {
                for (int i = 0; i < 5; i++) {
                    views.setTextViewTextSize(labelIds[i], TypedValue.COMPLEX_UNIT_DIP, 16);
                    views.setTextViewTextSize(tempIds[i], TypedValue.COMPLEX_UNIT_DIP, 18);
                }
            }
        }

        int fontColor = SettingsActivity.loadFontColorPref(context, appWidgetId);
        for (int i = 0; i < 5; i++) {
            views.setTextColor(labelIds[i], fontColor);
            views.setTextColor(tempIds[i], fontColor);
        }
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);
        TwWidgetProvider.setWidgetInfoStyle(context, appWidgetId, views);
        setWidgetHourlyStyle(context, appWidgetId, views);

        TwWidgetProvider.setPendingIntentToRefresh(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToSettings(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToApp(context, appWidgetId, views);
    }

    static public void setWidgetHourlyData(Context context, RemoteViews views, WidgetData wData) {
        if (wData == null) {
            Log.e(TAG, "weather data is NULL");
            return;
        }

        WeatherData currentData = wData.getCurrentWeather();
        if (currentData == null) {
            Log.e(TAG, "currentElement is NULL");
            return;
        }

        views.setTextViewText(R.id.label_now, context.getString(R.string.now));

        Calendar calendar = Calendar.getInstance();
        views.setTextViewText(R.id.now_temperature, String.valueOf(currentData.getTemperature())+"°");

        int skyResourceId = context.getResources().getIdentifier(currentData.getSkyImageName(), "drawable", context.getPackageName());
        if (skyResourceId == -1) {
            skyResourceId = R.drawable.sun;
        }
        views.setImageViewResource(R.id.now_sky, skyResourceId);

        for (int i=1; i<5; i++) {
            WeatherData hourlyData = wData.getHourlyWeather(i-1);

            calendar.setTime(hourlyData.getDate());
            String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY))
                    + context.getString(R.string.hour);
            views.setTextViewText(labelIds[i], hour);
            views.setTextViewText(tempIds[i], String.valueOf((int)hourlyData.getTemperature())+"°");

            if (hourlyData.getSky() != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                skyResourceId = context.getResources().getIdentifier(hourlyData.getSkyImageName(),
                        "drawable", context.getPackageName());
                if (skyResourceId == -1) {
                    skyResourceId = R.drawable.sun;
                }
                views.setImageViewResource(skyIds[i], skyResourceId);
            }
        }
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData) {
        TwWidgetProvider.setWidgetInfoData(context, views, wData);
        setWidgetHourlyData(context, views, wData);
    }
}

