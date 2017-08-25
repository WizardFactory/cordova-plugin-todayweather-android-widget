package net.wizardfactory.todayweather.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import net.wizardfactory.todayweather.MainActivity;
import net.wizardfactory.todayweather.R;

import org.apache.cordova.CordovaActivity;

/**
 * This class is menu of widget
 * used common all widget.
 */
public class WidgetMenuActivity extends CordovaActivity {
    private Context mContxt = null;
    private static String TAG = "WidgetMenu";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContxt = getApplicationContext();

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        //if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        //    Log.e(TAG, "invalid app widget id="+mAppWidgetId);
        //    finish();
        //    return;
        //}

        Log.i(TAG, "app widget id="+mAppWidgetId);
        setContentView(R.layout.widget_menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.menu_layout);
        mainLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                finish();
            }
        });

        // setting widget button
        Button settingsBtn = (Button)findViewById(R.id.setting_button);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                Intent intent = new Intent(mContxt, SettingsActivity.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContxt.startActivity(intent);
                finish();
            }
        });

        // update widget button
        Button updateBtn = (Button)findViewById(R.id.update_button);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                updateWidget();
                finish();
            }
        });

        // move main page button
        Button moveMainBtn = (Button)findViewById(R.id.move_main_button);
        moveMainBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                moveMain();
            }
        });
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContxt);

        int layoutId = appWidgetManager.getAppWidgetInfo(mAppWidgetId).initialLayout;
        Class<?> widgetProvider = WidgetUpdateService.getWidgetProvider(layoutId);

        Intent intent = new Intent(this, widgetProvider);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {mAppWidgetId});
        intent.putExtra("ManualUpdate", true);
        sendBroadcast(intent);
    }

    private void moveMain() {
        Intent intent = new Intent(mContxt, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContxt.startActivity(intent);
        finish();
    }
}
