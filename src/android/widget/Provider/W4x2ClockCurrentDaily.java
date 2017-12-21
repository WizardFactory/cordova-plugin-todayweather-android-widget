package net.wizardfactory.todayweather.widget.Provider;

import android.content.Context;
import android.widget.RemoteViews;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WidgetData;

/**
 * Implementation of App Widget functionality.
 */
public class W4x2ClockCurrentDaily extends ClockAndCurrentWeather {

    public W4x2ClockCurrentDaily() {
        TAG = "W4x2 ClockCurrentDaily";
        mLayoutId = R.layout.w4x2_clock_current_daily;
    }

    static public void setWidgetStyle(Context context, int appWidgetId, RemoteViews views) {
        TwWidgetProvider.setWidgetStyle(context, appWidgetId, views);
        TwWidgetProvider.setWidgetInfoStyle(context, appWidgetId, views);
        ClockAndCurrentWeather.setWidgetClockCurrentStyle(context, appWidgetId, views);
        DailyWeather.setWidgetDailyStyle(context, appWidgetId, views);

        TwWidgetProvider.setPendingIntentToRefresh(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToSettings(context, appWidgetId, views);
        TwWidgetProvider.setPendingIntentToApp(context, appWidgetId, views);
    }

    static public void setWidgetData(Context context, RemoteViews views, WidgetData wData, Units localUnits) {
        TwWidgetProvider.setWidgetInfoData(context, views, wData);
        ClockAndCurrentWeather.setWidgetClockCurrentData(context, views, wData, localUnits);
        DailyWeather.setWidgetDailyData(context, views, wData);
    }
}

