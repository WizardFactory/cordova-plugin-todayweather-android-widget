package net.wizardfactory.todayweather.widget.Provider;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.WeatherData;
import net.wizardfactory.todayweather.widget.Data.WidgetData;
import net.wizardfactory.todayweather.widget.JsonElement.WeatherElement;
import net.wizardfactory.todayweather.widget.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Implementation of App Widget functionality.
 */
public class AirQualityIndex extends TwWidgetProvider {

    public AirQualityIndex() {
        TAG = "W2x1 AirQualityIndex";
        mLayoutId = R.layout.air_quality_index;
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);
        TwWidgetProvider.setWidgetInfoStyle(context, appWidgetId, views);

        if (Build.MANUFACTURER.equals("samsung")) {
            if (Build.VERSION.SDK_INT >= 16) {
                views.setTextViewTextSize(R.id.label_aqi, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.label_pm10, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.label_pm25, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.aqi_str, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.pm10_str, TypedValue.COMPLEX_UNIT_DIP, 18);
                views.setTextViewTextSize(R.id.pm25_str, TypedValue.COMPLEX_UNIT_DIP, 18);
            }
        }

        int fontColor = SettingsActivity.loadFontColorPref(context, appWidgetId);
        views.setTextColor(R.id.errMsg, fontColor);
        views.setTextColor(R.id.label_aqi, fontColor);
        views.setTextColor(R.id.label_pm10, fontColor);
        views.setTextColor(R.id.label_pm25, fontColor);
        views.setTextColor(R.id.aqi_str, fontColor);
        views.setTextColor(R.id.pm10_str, fontColor);
        views.setTextColor(R.id.pm25_str, fontColor);

        TwWidgetProvider.setPendingIntentToRefresh(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToSettings(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToApp(context, appWidgetId, views);
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData) {
        if (wData == null) {
            Log.e(TAG, "weather data is NULL");
            return;
        }

        if (wData.getLoc() != null) {
            // setting town
            views.setTextViewText(R.id.location, wData.getLoc());
        }

        WeatherData currentData = wData.getCurrentWeather();
        if (currentData == null) {
            Log.e(TAG, "currentElement is NULL");
            return;
        }

        SimpleDateFormat transFormat = new SimpleDateFormat("HH:mm");
        views.setTextViewText(R.id.pubdate, transFormat.format(Calendar.getInstance().getTime()));

        int aqiGrade = currentData.getAqiGrade();
        int pm10Grade = currentData.getPm10Grade();
        int pm25Grade = currentData.getPm25Grade();

        if ((aqiGrade == WeatherElement.DEFAULT_WEATHER_INT_VAL || aqiGrade == 0)
                && (pm10Grade == WeatherElement.DEFAULT_WEATHER_INT_VAL || pm10Grade == 0)
                && (pm25Grade == WeatherElement.DEFAULT_WEATHER_INT_VAL || pm25Grade == 0)) {
            Log.i(TAG, "Fail to get aqi pub date");
            views.setTextViewText(R.id.errMsg, context.getString(R.string.this_location_is_not_supported));
            views.setViewVisibility(R.id.errMsg, View.VISIBLE);
            views.setViewVisibility(R.id.weather_layout, View.INVISIBLE);
            return;
        }

        views.setViewVisibility(R.id.weather_layout, View.VISIBLE);
        views.setViewVisibility(R.id.errMsg, View.INVISIBLE);

        if (aqiGrade != WeatherElement.DEFAULT_WEATHER_INT_VAL && aqiGrade != 0) {
            views.setTextViewText(R.id.label_aqi, context.getString(R.string.aqi));
            views.setTextViewText(R.id.aqi_str, String.valueOf(currentData.getAqiValue()));
            views.setImageViewResource(R.id.current_aqi_emoji, getDrawableFaceEmoji(aqiGrade));
        }
        if (pm10Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL && pm10Grade != 0) {
            views.setTextViewText(R.id.label_pm10, context.getString(R.string.pm10));
            views.setTextViewText(R.id.pm10_str, String.valueOf(currentData.getPm10Value()));
            views.setImageViewResource(R.id.current_pm10_emoji, getDrawableFaceEmoji(pm10Grade));
        }
        if (pm25Grade != WeatherElement.DEFAULT_WEATHER_INT_VAL && pm25Grade != 0) {
            views.setTextViewText(R.id.label_pm25, context.getString(R.string.pm25));
            views.setTextViewText(R.id.pm25_str, String.valueOf(currentData.getPm25Value()));
            views.setImageViewResource(R.id.current_pm25_emoji, getDrawableFaceEmoji(pm25Grade));
        }
    }

    static private int getDrawableFaceEmoji(int grade) {
        switch (grade) {
            case 1:
                return R.drawable.ic_sentiment_satisfied_white_48dp;
            case 2:
                return R.drawable.ic_sentiment_neutral_white_48dp;
            case 3:
                return R.drawable.ic_sentiment_dissatisfied_white_48dp;
            case 4:
                return R.drawable.ic_sentiment_very_dissatisfied_white_48dp;
        }
        return R.drawable.ic_sentiment_very_dissatisfied_white_48dp;
    }
}

