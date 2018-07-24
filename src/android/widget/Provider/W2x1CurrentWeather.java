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

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Implementation of App Widget functionality.
 */
public class W2x1CurrentWeather extends TwWidgetProvider {

    public W2x1CurrentWeather() {
        TAG = "W2x1 CurrentWeather";
        mLayoutId = R.layout.w2x1_current_weather;
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);
        TwWidgetProvider.setWidgetInfoStyle(context, appWidgetId, views);

        if (Build.MANUFACTURER.equals("samsung")) {
            if (Build.VERSION.SDK_INT >= 16) {
                views.setTextViewTextSize(R.id.today_temperature, TypedValue.COMPLEX_UNIT_DIP, 20);
                views.setTextViewTextSize(R.id.current_pm, TypedValue.COMPLEX_UNIT_DIP, 20);
                views.setTextViewTextSize(R.id.current_temperature, TypedValue.COMPLEX_UNIT_DIP, 48);
            }
        }

        int fontColor = SettingsActivity.loadFontColorPref(context, appWidgetId);
        views.setTextColor(R.id.today_temperature, fontColor);
        views.setTextColor(R.id.current_temperature, fontColor);
        views.setTextColor(R.id.current_pm, fontColor);

        //TwWidgetProvider.setPendingIntentToMenu(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToRefresh(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToSettings(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToApp(context, appWidgetId, views);
    }

    static public void setWidgetData(Context context, int appWidgetId, RemoteViews views, WidgetData wData, Units localUnits) {
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

        SimpleDateFormat transFormat = new SimpleDateFormat("HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(wData.dataTime);
        views.setTextViewText(R.id.pubdate, transFormat.format(calendar.getTime()));
        views.setTextViewText(R.id.current_temperature, String.valueOf(currentData.getTemperature())+"°");

        int skyResourceId = context.getResources().getIdentifier(currentData.getSkyImageName(), "drawable", context.getPackageName());
        if (skyResourceId == -1) {
            skyResourceId = R.drawable.sun;
        }
        views.setImageViewResource(R.id.current_sky, skyResourceId);

        double rn1 = currentData.getRn1();
        if (rn1 != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL && rn1 != 0 ) {
            views.setTextViewText(R.id.current_pm, " "+String.valueOf(rn1) + localUnits.getPrecipitationUnit());
        }
        else {
            int airInfoIndex = SettingsActivity.loadAirInfoIndexPref(context, appWidgetId);
            int grade = WeatherElement.DEFAULT_WEATHER_INT_VAL;
            String str = "";

            Log.i(TAG, "set2x1CurrentWeather airInfoIndex=" + airInfoIndex);
            if (airInfoIndex == 0) {
                grade = currentData.getAqiGrade();
                str = currentData.getAqiStr();
            }
            else if (airInfoIndex == 1) {
                grade = currentData.getPm25Grade();
                str = currentData.getPm25Str();
            }
            else if (airInfoIndex == 2) {
                grade = currentData.getPm10Grade();
                str = currentData.getPm10Str();
            }

            if (grade != WeatherElement.DEFAULT_WEATHER_INT_VAL) {
                views.setTextViewText(R.id.current_pm, " :::"+ str);
            }
        }

        int minTemperature = (int)currentData.getMinTemperature();
        int maxTemperature = (int)currentData.getMaxTemperature();

        String today_temperature = "";
        if (minTemperature != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL)  {
            today_temperature += String.valueOf(minTemperature)+"°";
        }
        if (maxTemperature != WeatherElement.DEFAULT_WEATHER_DOUBLE_VAL)  {
            today_temperature += "/";
            today_temperature += String.valueOf(maxTemperature)+"°";
        }
        views.setTextViewText(R.id.today_temperature, today_temperature);
    }
}

