package net.wizardfactory.todayweather.widget;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.Gson;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WidgetData;
import net.wizardfactory.todayweather.widget.JsonElement.AddressesElement;
import net.wizardfactory.todayweather.widget.JsonElement.GeoInfo;
import net.wizardfactory.todayweather.widget.JsonElement.WeatherElement;
import net.wizardfactory.todayweather.widget.JsonElement.WorldWeatherElement;
import net.wizardfactory.todayweather.widget.Provider.AirQualityIndex;
import net.wizardfactory.todayweather.widget.Provider.ClockAndCurrentWeather;
import net.wizardfactory.todayweather.widget.Provider.ClockAndThreeDays;
import net.wizardfactory.todayweather.widget.Provider.CurrentWeatherAndThreeDays;
import net.wizardfactory.todayweather.widget.Provider.DailyWeather;
import net.wizardfactory.todayweather.widget.Provider.W1x1CurrentWeather;
import net.wizardfactory.todayweather.widget.Provider.W2x1CurrentWeather;
import net.wizardfactory.todayweather.widget.Provider.W2x1WidgetProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * this class is widget update service.
 * find current position then request server to get weather data.
 * this result json string is parsed and draw to widget.
 */
public class WidgetUpdateService extends Service {
    // if find not location in this time, service is terminated.
    private final static int LOCATION_TIMEOUT = 30 * 1000; // 20sec
    private final static String SERVER_URL = "https://todayweather.wizardfactory.net";
    private final static String KMA_API_URL = "/v000803/town";
    private final static String WORLD_WEATHER_API_URL = "/ww/010000/current/2?gcode=";

    private LocationManager mLocationManager = null;
    private boolean mIsLocationManagerRemoveUpdates = false;
    /**
     * for user location widget
     */
    private int[] mAppWidgetId = new int[0];
    private Context mContext;
    private AppWidgetManager mAppWidgetManager;
    private int mLayoutId;

    private Units mLocalUnits = null;

    static final int MSG_GET_GEOINFO = 1;
    static final int MSG_GET_KR_ADDRESS = 2;
    static final int MSG_GET_WEATHER_INFO = 3;
    static final int MSG_DRAW_WIDGET = 4;

    private Handler mHandler = null;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_GEOINFO:
                    getGeoInfo(msg.arg1);
                    break;
                case MSG_GET_KR_ADDRESS:
                    getKrAddressInfo(msg.arg1);
                    break;
                case MSG_GET_WEATHER_INFO:
                    getWeatherInfo(msg.arg1);
                    break;
                case MSG_DRAW_WIDGET:
                    updateWidget(msg.arg1);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    class TransWeather {
        public int widgetId  = AppWidgetManager.INVALID_APPWIDGET_ID;
        public GeoInfo geoInfo = null;
//        public String urlWeatherInfo = null;
        public String strJsonWeatherInfo = null;
        public boolean currentPosition = false;
    }

    private List<TransWeather> mTransWeatherInfoList = new ArrayList<TransWeather>();

    private TransWeather getTransWeatherInfo(int widgetId) {
        if (mTransWeatherInfoList.isEmpty()) {
            TransWeather transWeather = new TransWeather();
            transWeather.widgetId = widgetId;
            mTransWeatherInfoList.add(transWeather);
            return transWeather;
        }
        else {
            for(TransWeather transWeather : mTransWeatherInfoList){
                if (transWeather.widgetId == widgetId) {
                    return transWeather;
                }
            }

            Log.i("Service", "add trans weather " + widgetId);
            TransWeather transWeather = new TransWeather();
            transWeather.widgetId = widgetId;
            mTransWeatherInfoList.add(transWeather);
            return transWeather;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "on Start Command");

        if (intent == null) {
            Log.e("Service", "intent is null on Start Command");
            return START_NOT_STICKY;
        }

        // Find the widget id from the intent.
        Bundle extras = intent.getExtras();
        int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            widgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return START_NOT_STICKY;
        }

        mHandler = new IncomingHandler();

        startUpdate(widgetId);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startUpdate(final int widgetId) {
        final Context context = getApplicationContext();
        String jsonCityInfoStr = SettingsActivity.loadCityInfoPref(context, widgetId);
        boolean currentPosition = false;
        JSONObject location = null;
        GeoInfo geoInfo = new GeoInfo();

        if (jsonCityInfoStr == null) {
            Log.i("Service", "cityInfo is null, so this widget is zombi");

//            Intent result = new Intent();
//            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
//            result.setAction(AppWidgetManager.ACTION_APPWIDGET_DELETED);
//            context.sendBroadcast(result);
            return;
        }

        Units units = SettingsActivity.loadUnitsPref(context);

        try {
            JSONObject jsonCityInfo = new JSONObject(jsonCityInfoStr);
            currentPosition = jsonCityInfo.getBoolean("currentPosition");
            geoInfo.setAddress(jsonCityInfo.get("address").toString());
            if (jsonCityInfo.has("country")) {
                geoInfo.setCountry(jsonCityInfo.get("country").toString());
            }
            else {
                geoInfo.setCountry("KR");
            }
            if (jsonCityInfo.has("location") && !jsonCityInfo.isNull("location")) {
                location = jsonCityInfo.getJSONObject("location");
                geoInfo.setLat(geoInfo.toNormalize(location.getDouble("lat")));
                geoInfo.setLng(geoInfo.toNormalize(location.getDouble("long")));
            }
            if (jsonCityInfo.has("name") && !jsonCityInfo.isNull("name")) {
                geoInfo.setName(jsonCityInfo.getString("name"));
            }

            mLocalUnits = units;
        } catch (JSONException e) {
            Log.e("Service", "JSONException: " + e.getMessage());
            return;
        }

        TransWeather transWeather = getTransWeatherInfo(widgetId);
        transWeather.geoInfo = geoInfo;
        transWeather.currentPosition = currentPosition;


        if (currentPosition) {
            Log.i("Service", "Update current position app widget id=" + widgetId);
            /**
             * current geo inf를 가장 최근 위치값으로 본다.
             * 실제로 앱에서 최종 갱신일 수 있지만, 여기서는 이 값을 가장 최신으로 본다.
             * registerLocationUpdates에서 한 번 더 갱신된다.
             */
            GeoInfo savedGeoInfo = this.loadCurrentGeoInfo(context);
            if (savedGeoInfo != null && savedGeoInfo.getName() != null) {
                transWeather.geoInfo = geoInfo;
            }
            registerLocationUpdates(widgetId);

            // if location do not found in LOCATION_TIMEOUT, this service is stop.
            Runnable stopLocationListenerRunnable = new Runnable() {
                public void run() {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    }
                    else {
                        if (mLocationManager != null) {
                            mLocationManager.removeUpdates(locationListener);
                        }
                    }
                    stopSelf();
                }
            };
            Handler stopLocationListenerHandler = new Handler();
            stopLocationListenerHandler.postDelayed(stopLocationListenerRunnable, LOCATION_TIMEOUT);
        } else {
            Log.i("Service", "Update address=" + geoInfo.getAddress() + " app widget id=" + widgetId);

            mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, -1));
        }
    }

    private void registerLocationUpdates(final int widgetId) {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            // once widget update by last location
            Location lastLoc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastLoc != null) {
                Log.i("Service", "success last location from NETWORK");
            } else {
                lastLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Log.i("Service", "success last location from gps");
            }

            TransWeather transWeather = getTransWeatherInfo(widgetId);

            if (lastLoc != null) {
                if (transWeather.geoInfo == null) {
                    transWeather.geoInfo = new GeoInfo();
                }
                transWeather.geoInfo.setLat(transWeather.geoInfo.toNormalize(lastLoc.getLatitude()));
                transWeather.geoInfo.setLng(transWeather.geoInfo.toNormalize(lastLoc.getLongitude()));
            }
            else {
                Log.i("Service", "use saved location info");
            }

            if (transWeather.geoInfo != null) {
                mHandler.sendMessage(Message.obtain(null, MSG_GET_GEOINFO, widgetId, -1));
            }

            mAppWidgetId = Arrays.copyOf(mAppWidgetId, mAppWidgetId.length + 1);
            mAppWidgetId[mAppWidgetId.length - 1] = widgetId;

            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 300, 0, locationListener);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

        } catch (SecurityException e) {
            Log.e("Service", e.toString());
            e.printStackTrace();
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            final double lon = location.getLongitude();
            final double lat = location.getLatitude();

            Log.e("Service", "Loc listen lat : " + lat + ", lon: " + lon + " provider " + location.getProvider());

            if (mIsLocationManagerRemoveUpdates == false) {
                // for duplicated call do not occur.
                // flag setting and method call.
                mIsLocationManagerRemoveUpdates = true;

                final Context context = getApplicationContext();
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLocationManager.removeUpdates(locationListener);

                for (int i = 0; i < mAppWidgetId.length; i++) {
                    getTransWeatherInfo(mAppWidgetId[i]).geoInfo = new GeoInfo();
                    GeoInfo geoInfo = getTransWeatherInfo(mAppWidgetId[i]).geoInfo;
                    geoInfo.setLat(geoInfo.toNormalize(lat));
                    geoInfo.setLng(geoInfo.toNormalize(lon));
                    mHandler.sendMessage(Message.obtain(null, MSG_GET_GEOINFO, mAppWidgetId[i], -1));
                }
                mAppWidgetId = Arrays.copyOf(mAppWidgetId, 0);
                stopSelf();
            }
        }
        public void onProviderDisabled(String provider) {
        }
        public void onProviderEnabled(String provider) {
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private static final String WIDGET_PREFS_NAME = "net.wizardfactory.todayweather.widget.Provider.WidgetProvider";
    private GeoInfo loadCurrentGeoInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, 0);
        Gson gson = new Gson();
        String json = prefs.getString("CurrentGeoInfo", "");
        GeoInfo geoInfo = gson.fromJson(json, GeoInfo.class);

        return geoInfo;
    };

    private void saveCurrentGeoInfo(Context context, GeoInfo geoInfo) {
        if (geoInfo == null || geoInfo.getName() == null || geoInfo.getLat() == 0 || geoInfo.getLng() == 0)  {
            if (geoInfo == null) {
                Log.e("Service", "Geo info is null");
            }
            else {
                Log.e("Service", "Invalid geo info ="+geoInfo.toString());
            }
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(WIDGET_PREFS_NAME, 0);
        Gson gson = new Gson();
        String json = prefs.getString("CurrentGeoInfo", "");
        GeoInfo savedGeoInfo = gson.fromJson(json, GeoInfo.class);
        if(savedGeoInfo != null && savedGeoInfo.getLng() == geoInfo.getLng() && savedGeoInfo.getLat() == geoInfo.getLat()) {
            Log.i("Service", "It's the same as saved geo info="+savedGeoInfo.toString());
            return;
        }

        SharedPreferences.Editor prefsEditor = prefs.edit();
        json = gson.toJson(geoInfo);
        prefsEditor.putString("CurrentGeoInfo", json);
        prefsEditor.commit();

        Log.i("Service", "Save geo info ="+geoInfo.toString());
    };

    /**
     *
     // Tokyo 35.6894875,139.6917064
     // position = {coords: {latitude: 35.6894875, longitude: 139.6917064}};
     // Shanghai 31.227797,121.475194
     //position = {coords: {latitude: 31.227797, longitude: 121.475194}};
     // NY 40.663527,-73.960852
     //position = {coords: {latitude: 40.663527, longitude: -73.960852}};
     // Berlin 52.516407,13.403322
     //position = {coords: {latitude: 52.516407, longitude: 13.403322}};
     // Hochinminh 10.779001,106.662796
     //position = {coords: {latitude: 10.779001, longitude: 106.662796}};
     *
     * @param widgetId
     */
    private void getGeoInfo(final int widgetId) {
        //load saved data
        //if geoinfo is same with saved
        //call msg_get_weather_info

        GeoInfo savedGeoInfo = this.loadCurrentGeoInfo(getApplicationContext());
        if(savedGeoInfo != null && savedGeoInfo.getLat() != 0) {
           if (savedGeoInfo.getLat() == getTransWeatherInfo(widgetId).geoInfo.getLat() &&
                   savedGeoInfo.getLng() == getTransWeatherInfo(widgetId).geoInfo.getLng()) {
               if (savedGeoInfo.getName() != null) {
                   Log.i("Service", "Use saved geoinfo="+savedGeoInfo.toString());
                   mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, -1));
                   return;
               }
           }
        }

        String lang = Locale.getDefault().getLanguage();
        String geoUrl = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%.3f,%.3f&language=%s",
                getTransWeatherInfo(widgetId).geoInfo.getLat(), getTransWeatherInfo(widgetId).geoInfo.getLng(), lang);

        new GetHttpsServerAysncTask(geoUrl, new AsyncCallback() {
            @Override
            public void onPostExecute(String jsonStr) {
                try {
                    TransWeather transWeather = getTransWeatherInfo(widgetId);
                    if (transWeather.geoInfo == null) {
                        transWeather.geoInfo = new GeoInfo();
                    }
                    if (transWeather.geoInfo.setJsonString(jsonStr) != true) {
                        throw new Exception("Fail to parse geo info");
                    }

                    Log.i("Service", transWeather.geoInfo.getAddress());

                    if (transWeather.geoInfo.getCountry().equals("KR")) {
                        mHandler.sendMessage(Message.obtain(null, MSG_GET_KR_ADDRESS, widgetId, -1));
                    }
                    else {
                        mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, -1));
                    }
                }
                catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.fail_to_get_location, Toast.LENGTH_LONG).show();
                    Log.e("Service", e.toString());
                }
            }
        }).execute();
    }

    private void getKrAddressInfo(final int widgetId) {
        Double lat = getTransWeatherInfo(widgetId).geoInfo.getLat();
        Double lng = getTransWeatherInfo(widgetId).geoInfo.getLng();
        Log.i("Service", "lat : " + lat + ", lng " + lng);
        String geoUrl = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%.3f,%.3f&language=ko", lat, lng);
        new GetHttpsServerAysncTask(geoUrl, new AsyncCallback() {
            @Override
            public void onPostExecute(String jsonStr) {
                String retAddr = null;
                try {
                    AddressesElement addrsElement = AddressesElement.parsingAddressJson2String(jsonStr);
                    if (addrsElement == null || addrsElement.getAddrs() == null) {
                        throw new Exception("Fail to parse address json to string");
                    }
                    retAddr = addrsElement.findDongAddressFromGoogleGeoCodeResults();
                    if (retAddr == null) {
                        throw new Exception("Fail to find dong address from google geo code");
                    }
                    getTransWeatherInfo(widgetId).geoInfo.setAddress(retAddr);
                    mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, -1));
                }
                catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.fail_to_get_location, Toast.LENGTH_LONG).show();
                    Log.e("Service", e.toString());
                }
            }
        }).execute();
    }

    private void getWeatherInfo(final int widgetId) {
        Log.i("WidgetUpdateService", "get weather info widget="+widgetId);

        TransWeather transWeather = getTransWeatherInfo(widgetId);
        if (transWeather.currentPosition) {
            this.saveCurrentGeoInfo(getApplicationContext(), transWeather.geoInfo);
        }

        GeoInfo geoInfo = transWeather.geoInfo;
        String url = null;
        Log.i("WidgetUpdateService", "get weather info geo info="+geoInfo.toString());

        if (geoInfo.getCountry() == null || geoInfo.getCountry().equals("KR")) {
            String addr = AddressesElement.makeUrlAddress(geoInfo.getAddress());
            if (addr != null) {
                url = SERVER_URL + KMA_API_URL + addr;
                Log.i("Service", "url=" + url);
            }
        }
        else {
            url = SERVER_URL + WORLD_WEATHER_API_URL + geoInfo.getLat() + "," + geoInfo.getLng();
            Log.i("Service", "url=" + url);
        }

        if (url == null) {
            Log.e("Service", "url is null on get weather info");
            return;
        }

        new GetHttpsServerAysncTask(url, new AsyncCallback() {
            @Override
            public void onPostExecute(String jsonStr) {
                if (jsonStr != null && jsonStr.length() > 0) {
                    getTransWeatherInfo(widgetId).strJsonWeatherInfo = jsonStr;
                    mHandler.sendMessage(Message.obtain(null, MSG_DRAW_WIDGET, widgetId, -1));
                }
                else {
                    Log.e("Service", "weather json string is null or zero");
                }
            }
        }).execute();
    }

    private void updateWidget(int widgetId) {
        Log.i("WidgetUpdateService", "update widget="+widgetId);

        TransWeather transWeather = getTransWeatherInfo(widgetId);
        if (transWeather.geoInfo.getCountry() == null || transWeather.geoInfo.getCountry().equals("KR")) {
            updateKrWeatherWidget(widgetId, transWeather.strJsonWeatherInfo, transWeather.geoInfo.getName());
        }
        else {
            updateWorldWeatherWidget(widgetId, transWeather.strJsonWeatherInfo, transWeather.geoInfo.getName());
        }
    }

    private void updateWorldWeatherWidget(int widgetId, String jsonStr, String locationName) {
        if (jsonStr == null) {
            Log.e("WidgetUpdateService", "jsonData is NULL");
            Toast.makeText(getApplicationContext(), "Fail to get world weather data", Toast.LENGTH_LONG).show();
            return;
        }

        /**
         * context, manager를 member 변수로 변환하면 단순해지지만 동작 확인 필요.
         */
        try {
            mContext = getApplicationContext();
            mAppWidgetManager = AppWidgetManager.getInstance(mContext);
            mLayoutId = mAppWidgetManager.getAppWidgetInfo(widgetId).initialLayout;
        } catch (Exception e) {
            Log.e("Service", "Exception: " + e.getMessage());
            return;
        }

        Context context = getApplicationContext();
        // input weather content to widget layout

        RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);

        WidgetData wData = new WidgetData();
        if (locationName != null) {
            wData.setLoc(locationName);
        }
        wData.setUnits(WorldWeatherElement.getUnits(jsonStr));

        if (mLayoutId == R.layout.w2x1_widget_layout) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            wData.setBefore24hWeather(WorldWeatherElement.getBefore24hWeather(jsonStr));
            W2x1WidgetProvider.setWidgetStyle(context, widgetId, views);
            W2x1WidgetProvider.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set2x1WidgetData id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w1x1_current_weather) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            W1x1CurrentWeather.setWidgetStyle(context, widgetId, views);
            W1x1CurrentWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set 1x1WidgetData id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w2x1_current_weather) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            W2x1CurrentWeather.setWidgetStyle(context, widgetId, views);
            W2x1CurrentWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set 2x1 current weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.air_quality_index) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            AirQualityIndex.setWidgetStyle(context, widgetId, views);
            AirQualityIndex.setWidgetData(context, views, wData);
            Log.i("UpdateWorldWeather", "set air quality index id=" + widgetId);
        }
        else if (mLayoutId == R.layout.clock_and_current_weather) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            ClockAndCurrentWeather.setWidgetStyle(context, widgetId, views);
            ClockAndCurrentWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set 3x1 clock and current weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.current_weather_and_three_days) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            wData.setDayWeather(0, WorldWeatherElement.getDayWeatherFromToday(jsonStr, -1));
            wData.setDayWeather(1, WorldWeatherElement.getDayWeatherFromToday(jsonStr, 0));
            wData.setDayWeather(2, WorldWeatherElement.getDayWeatherFromToday(jsonStr, 1));
            CurrentWeatherAndThreeDays.setWidgetStyle(context, widgetId, views);
            CurrentWeatherAndThreeDays.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set 3x1 clock and current weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.daily_weather) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            wData.setDayWeather(0, WorldWeatherElement.getDayWeatherFromToday(jsonStr, -1));
            for (int i=0; i<5; i++) {
                wData.setDayWeather(i, WorldWeatherElement.getDayWeatherFromToday(jsonStr, i-1));
            }
            DailyWeather.setWidgetStyle(context, widgetId, views);
            DailyWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set 4x1 daily weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.clock_and_three_days) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            wData.setDayWeather(0, WorldWeatherElement.getDayWeatherFromToday(jsonStr, -1));
            wData.setDayWeather(1, WorldWeatherElement.getDayWeatherFromToday(jsonStr, 0));
            wData.setDayWeather(2, WorldWeatherElement.getDayWeatherFromToday(jsonStr, 1));
            ClockAndThreeDays.setWidgetStyle(context, widgetId, views);
            ClockAndThreeDays.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWorldWeather", "set 4x1 clock and three days id=" + widgetId);
        }

        // Create an Intent to launch menu
        Intent intent = new Intent(context, WidgetMenuActivity.class);
        intent.putExtra("LAYOUT_ID", mLayoutId);
        // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.bg_layout, pendingIntent);

        mAppWidgetManager.updateAppWidget(widgetId, views);
    }

    /**
     * location listener에서 widgetId가 업데이트됨.
     * @param widgetId
     * @param jsonStr
     */
    private void updateKrWeatherWidget(int widgetId, String jsonStr, String locationName) {
        if (jsonStr == null) {
            Log.e("WidgetUpdateService", "jsonData is NULL");
            return;
        }
        Log.i("Service", "jsonStr: " + jsonStr);

        // parsing json string to weather class
        WeatherElement weatherElement = WeatherElement.parsingWeatherElementString2Json(jsonStr);
        if (weatherElement == null) {
            Log.e("WidgetUpdateService", "weatherElement is NULL");
            return;
        }

        /**
         * context, manager를 member 변수로 변환하면 단순해지지만 동작 확인 필요.
         */
        try {
            mContext = getApplicationContext();
            mAppWidgetManager = AppWidgetManager.getInstance(mContext);
            mLayoutId = mAppWidgetManager.getAppWidgetInfo(widgetId).initialLayout;
        } catch (Exception e) {
            Log.e("Service", "Exception: " + e.getMessage());
            return;
        }

        // make today, yesterday weather info class
        WidgetData wData = weatherElement.makeWidgetData();
        if (locationName != null) {
            wData.setLoc(locationName);
        }
        Context context = getApplicationContext();
        // input weather content to widget layout

        RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);

        if (mLayoutId == R.layout.w2x1_widget_layout) {
            W2x1WidgetProvider.setWidgetStyle(context, widgetId, views);
            W2x1WidgetProvider.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set2x1WidgetData id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w1x1_current_weather) {
            W1x1CurrentWeather.setWidgetStyle(context, widgetId, views);
            W1x1CurrentWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 1x1WidgetData id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w2x1_current_weather) {
            W2x1CurrentWeather.setWidgetStyle(context, widgetId, views);
            W2x1CurrentWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 2x1 current weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.air_quality_index) {
            AirQualityIndex.setWidgetStyle(context, widgetId, views);
            AirQualityIndex.setWidgetData(context, views, wData);
            Log.i("UpdateWidgetService", "set air quality index id=" + widgetId);
        }
        else if (mLayoutId == R.layout.clock_and_current_weather) {
            ClockAndCurrentWeather.setWidgetStyle(context, widgetId, views);
            ClockAndCurrentWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 3x1 clock and current weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.current_weather_and_three_days) {
            CurrentWeatherAndThreeDays.setWidgetStyle(context, widgetId, views);
            CurrentWeatherAndThreeDays.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 3x1 clock and current weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.daily_weather) {
            DailyWeather.setWidgetStyle(context, widgetId, views);
            DailyWeather.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x1 daily weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.clock_and_three_days) {
            ClockAndThreeDays.setWidgetStyle(context, widgetId, views);
            ClockAndThreeDays.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x1 clock and three days id=" + widgetId);
        }

        // Create an Intent to launch menu
        Intent intent = new Intent(context, WidgetMenuActivity.class);
        intent.putExtra("LAYOUT_ID", mLayoutId);
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.bg_layout, pendingIntent);

        mAppWidgetManager.updateAppWidget(widgetId, views);
    }

    static public Class<?> getWidgetProvider(int layoutId) {
        if (layoutId == R.layout.w2x1_widget_layout) {
            return W2x1WidgetProvider.class;
        } else if (layoutId == R.layout.w1x1_current_weather) {
            return W1x1CurrentWeather.class;
        } else if (layoutId == R.layout.w2x1_current_weather) {
            return W2x1CurrentWeather.class;
        } else if (layoutId == R.layout.air_quality_index) {
            return AirQualityIndex.class;
        } else if (layoutId == R.layout.clock_and_current_weather) {
            return ClockAndCurrentWeather.class;
        } else if (layoutId == R.layout.current_weather_and_three_days) {
            return CurrentWeatherAndThreeDays.class;
        } else if (layoutId == R.layout.daily_weather) {
            return DailyWeather.class;
        } else if (layoutId == R.layout.clock_and_three_days) {
            return ClockAndThreeDays.class;
        }

        return null;
    }
}// class end
