package net.wizardfactory.todayweather.widget.Provider;

import android.content.Context;
import android.widget.RemoteViews;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WidgetData;

/**
 * Implementation of App Widget functionality.
 */
public class W4x3ClockCurrentHourlyDaily extends W4x2ClockCurrentHourly {
    public W4x3ClockCurrentHourlyDaily() {
        TAG = "W4x3 ClockCurrentHourlyDaily";
        mLayoutId = R.layout.w4x2_clock_current_hourly;
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        W4x2ClockCurrentHourly.setWidgetStyle(context, appWidgetId, views);
        DailyWeather.setWidgetDailyStyle(context, appWidgetId, views);
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData, Units localUnits) {
        W4x2ClockCurrentHourly.setWidgetData(context, views, wData, localUnits);
        DailyWeather.setWidgetDailyData(context, views, wData);
    }

}

