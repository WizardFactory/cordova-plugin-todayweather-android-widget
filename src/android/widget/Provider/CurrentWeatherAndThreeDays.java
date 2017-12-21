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

/**
 * Implementation of App Widget functionality.
 */
public class CurrentWeatherAndThreeDays extends ClockAndCurrentWeather {
    static int[] labelIds = {R.id.label_yesterday, R.id.label_today, R.id.label_tomorrow};
    static int[] tempIds = {R.id.yesterday_temperature, R.id.today_temperature, R.id.tomorrow_temperature};
    static int[] skyIds = {R.id.yesterday_sky, R.id.today_sky, R.id.tomorrow_sky};

    public CurrentWeatherAndThreeDays() {
        TAG = "W4x1 CurrentWeatherAndThreeDays";
        mLayoutId = R.layout.current_weather_and_three_days;
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

        views.setTextViewText(R.id.label_yesterday, context.getString(R.string.yesterday));
        views.setTextViewText(R.id.label_today, context.getString(R.string.today));
        views.setTextViewText(R.id.label_tomorrow, context.getString(R.string.tomorrow));

        for (int i=0; i<3; i++) {
            WeatherData dayData = wData.getDayWeather(i);
            int minTemperature = (int) dayData.getMinTemperature();
            int maxTemperature = (int) dayData.getMaxTemperature();
            String day_temperature = "";
            if (minTemperature != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL) {
                day_temperature += String.valueOf(minTemperature)+"°";;
            }
            if (maxTemperature != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL) {
                day_temperature += "/";
                day_temperature += String.valueOf(maxTemperature)+"°";;
            }
            views.setTextViewText(tempIds[i], day_temperature);

            if (dayData.getSky() != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                skyResourceId = context.getResources().getIdentifier(dayData.getSkyImageName(true), "drawable", context.getPackageName());
                if (skyResourceId == -1) {
                    skyResourceId = R.drawable.sun;
                }
                views.setImageViewResource(skyIds[i], skyResourceId);
            }
        }
    }

}

