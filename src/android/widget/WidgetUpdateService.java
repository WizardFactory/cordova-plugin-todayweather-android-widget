package net.wizardfactory.todayweather.widget;

import android.app.Notification;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.gson.Gson;

import net.wizardfactory.todayweather.R;
import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WeatherData;
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
import net.wizardfactory.todayweather.widget.Provider.W4x1Current3Hourly;
import net.wizardfactory.todayweather.widget.Provider.W4x1Hourly;
import net.wizardfactory.todayweather.widget.Provider.W4x2ClockCurrentDaily;
import net.wizardfactory.todayweather.widget.Provider.W4x2ClockCurrentHourly;
import net.wizardfactory.todayweather.widget.Provider.W4x3ClockCurrentHourlyDaily;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * this class is widget update service.
 * find current position then request server to get weather data.
 * this result json string is parsed and draw to widget.
 */
public class WidgetUpdateService extends Service {
    private final static String SERVER_URL = "https://todayweather.wizardfactory.net";
    //private final static String KMA_API_URL = "/v000803/town";
    //private final static String WORLD_WEATHER_API_URL = "/ww/010000/current/2?gcode=";
    private final static String GEOINFO_TO_WEATHER_API_URL = "/weather/coord";
    private final static String KMA_ADDR_API_URL = "/v000901/kma/addr";

    private LocationManager mLocationManager = null;
    private Context mContext;
    private AppWidgetManager mAppWidgetManager;
    private int mLayoutId;

    private Units mLocalUnits = null;

//    static final int MSG_GET_GEOINFO = 1;
//    static final int MSG_GET_KR_ADDRESS = 2;
    static final int MSG_GET_WEATHER_INFO = 3;
    static final int MSG_DRAW_WIDGET = 4;

    private Handler mHandler = null;

    /**
     * msg.arg1 is widgetId, msg.arg2 is startId
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            if (isNetworkConnected(getApplicationContext()) == false) {
                //keep current state
                keepUpdateState(msg.what, msg.arg1, msg.arg2);
                return;
            }

            switch (msg.what) {
//                case MSG_GET_GEOINFO:
//                    getGeoInfo(msg.arg1, msg.arg2);
//                    break;
//                case MSG_GET_KR_ADDRESS:
//                    getKrAddressInfo(msg.arg1, msg.arg2);
//                    break;
                case MSG_GET_WEATHER_INFO:
                    getWeatherInfo(msg.arg1, msg.arg2);
                    break;
                case MSG_DRAW_WIDGET:
                    updateWidget(msg.arg1, msg.arg2);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    class TransWeather {
        public int widgetId  = AppWidgetManager.INVALID_APPWIDGET_ID;
        public GeoInfo geoInfo = null;
        public String strJsonWeatherInfo = null;
        public boolean currentPosition = false;
        public int startId;
        public int msgWhat;
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

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Service", "on Create");
        startForeground(1, new Notification());

        mHandler = new IncomingHandler();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("Service", "on Receive from broadcast receiver");
                if (isNetworkConnected(getApplicationContext())  == true) {
                    for(TransWeather transWeather : mTransWeatherInfoList){
                        if (transWeather.msgWhat >= MSG_GET_WEATHER_INFO) {
                            Log.i("Service", "retry widgetId:"+transWeather.widgetId+", startId="+transWeather.startId+", what="+transWeather.msgWhat);
                            mHandler.sendMessage(Message.obtain(null, transWeather.msgWhat, transWeather.widgetId, transWeather.startId));
                        }
                    }
                }
            }
        };

        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        Log.i("Service", "on Destroy");
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Service", "on Start Command");

        if (intent == null) {
            Log.e("Service", "intent is null on Start Command");
            stopSelf(startId);
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
            Log.e("Service", "INVALID APP WIDGET ID");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        startUpdate(widgetId, startId);

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
        }
        catch (Exception e) {
            Log.e("Service", "Fail to get active network info");
            e.printStackTrace();
            return false;
        }
    }

    private void startUpdate(final int widgetId, final int startId) {
        final Context context = getApplicationContext();
        String jsonCityInfoStr = SettingsActivity.loadCityInfoPref(context, widgetId);
        boolean currentPosition = false;
        JSONObject location = null;
        GeoInfo geoInfo = new GeoInfo();

        if (jsonCityInfoStr == null) {
            Log.e("Service", "cityInfo is null, so this widget is zombi");
            stopSelf(startId);
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
            stopSelf(startId);
            return;
        }

        Log.i("Service", "start update startId="+startId);

        //notice start updating
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int layoutId = appWidgetManager.getAppWidgetInfo(widgetId).initialLayout;
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
            views.setTextViewText(R.id.pubdate, "Loading");
            appWidgetManager.updateAppWidget(widgetId, views);
        }
        catch (Exception e) {
            Log.e("Service", e.toString());
            e.printStackTrace();
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
            registerLocationUpdates(widgetId, startId);
       } else {
            Log.i("Service", "Update address=" + geoInfo.getAddress() + " app widget id=" + widgetId);

            mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId));
        }
    }

    private void registerLocationUpdates(final int widgetId, final int startId) {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            // once widget update by last location
            Location lastLoc = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (lastLoc != null) {
                Log.i("Service", "success last location from PASSIVE");
            } else {
                lastLoc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastLoc != null) {
                    Log.i("Service", "success last location from NETWORK");
                }
                else {
                    lastLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(lastLoc != null) {
                        Log.i("Service", "success last location from GPS");
                    }
                }
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
                mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, -1));
            }

            Criteria criteria = new Criteria();
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAccuracy(Criteria.ACCURACY_FINE);

            mLocationManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    final double lon = location.getLongitude();
                    final double lat = location.getLatitude();
                    Log.i("onLocationChanged", "widgetId: "+widgetId+" startId: "+startId+", Loc listen lat: " + lat + ", lon: " + lon + ", provider: " + location.getProvider());

                    TransWeather transWeather = getTransWeatherInfo(widgetId);
                    transWeather.geoInfo.setLat(transWeather.geoInfo.toNormalize(location.getLatitude()));
                    transWeather.geoInfo.setLng(transWeather.geoInfo.toNormalize(location.getLongitude()));

                    mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId));
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
                @Override
                public void onProviderEnabled(String provider) {
                }
                @Override
                public void onProviderDisabled(String provider) {
                }
            }, null);

        } catch (SecurityException e) {
            Log.e("Service", e.toString());
            e.printStackTrace();
            stopSelf(startId);
        }
    }

    private void keepUpdateState(int what, int widgetId, int startId) {
        Log.i("Service", "keep transmission state widgetId:" + widgetId + ", startId:"+startId+", what="+what);
        TransWeather transWeather = getTransWeatherInfo(widgetId);
        transWeather.startId = startId;
        transWeather.msgWhat = what;
        return;
    }

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
    private void getGeoInfo(final int widgetId, final int startId) {
        //load saved data
        //if geoinfo is same with saved
        //call msg_get_weather_info

        GeoInfo savedGeoInfo = this.loadCurrentGeoInfo(getApplicationContext());
        if (savedGeoInfo == null) {
            Log.i("UpdateService", "savedGeoinfo: null");
        }
        else {
            Log.i("UpdateService", "savedGeoinfo:"+savedGeoInfo.toString());
        }

        if((savedGeoInfo != null) && (savedGeoInfo.getLat() != 0)) {
            if (savedGeoInfo.getLat() == getTransWeatherInfo(widgetId).geoInfo.getLat() &&
                    savedGeoInfo.getLng() == getTransWeatherInfo(widgetId).geoInfo.getLng()) {
                if (savedGeoInfo.getName() != null) {
                    Log.i("Service", "Use saved geoinfo="+savedGeoInfo.toString());
                    getTransWeatherInfo(widgetId).geoInfo = savedGeoInfo;
                    mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId));
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
                        mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId));
                    }
                    else {
                        mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId));
                    }
                }
                catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.fail_to_get_location, Toast.LENGTH_LONG).show();
                    Log.e("Service", e.toString());

                    mHandler.sendMessageDelayed(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId), 60000);
                }
            }
        }).execute();
    }

    private void getKrAddressInfo(final int widgetId, final int startId) {
        Double lat = getTransWeatherInfo(widgetId).geoInfo.getLat();
        Double lng = getTransWeatherInfo(widgetId).geoInfo.getLng();
        Log.i("Service", "kr address info lat : " + lat + ", lng " + lng);
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
                    mHandler.sendMessage(Message.obtain(null, MSG_GET_WEATHER_INFO, widgetId, startId));
                }
                catch (Exception e) {
                    Toast.makeText(getApplicationContext(), R.string.fail_to_get_location, Toast.LENGTH_LONG).show();
                    Log.e("Service", e.toString());
                    stopSelf(startId);
                }
            }
        }).execute();
    }

    private void getWeatherInfo(final int widgetId, final int startId) {
        Log.i("WidgetUpdateService", "get weather info widget="+widgetId);

        TransWeather transWeather = getTransWeatherInfo(widgetId);

        GeoInfo geoInfo = transWeather.geoInfo;
        Log.i("WidgetUpdateService", "get weather info geo info="+geoInfo.toString());
        String url = null;
        if (geoInfo.getLat() == 0 && geoInfo.getLng() == 0) {
            String addr = AddressesElement.makeUrlAddress(geoInfo.getAddress());
            if (addr != null) {
                url = SERVER_URL + KMA_ADDR_API_URL + addr;
            }
        }
        else {
            url = SERVER_URL + GEOINFO_TO_WEATHER_API_URL + "/" + geoInfo.getLat() + "," + geoInfo.getLng();
        }

        url += "?"+"temperatureUnit="+mLocalUnits.getTemperatureUnit();
        url += "&"+"distanceUnit="+mLocalUnits.getDistanceUnit();
        url += "&"+"airUnit="+mLocalUnits.getAirUnit();
        url += "&"+"precipitationUnit="+mLocalUnits.getPrecipitationUnit();
        url += "&"+"pressureUnit="+mLocalUnits.getPressureUnit();
        url += "&"+"windSpeedUnit="+mLocalUnits.getWindSpeedUnit();

        Log.i("Service", "url=" + url);

        if (url == null) {
            Log.e("Service", "url is null on get weather info");
            stopSelf(startId);
            return;
        }

        new GetHttpsServerAysncTask(url, new AsyncCallback() {
            @Override
            public void onPostExecute(String jsonStr) {
                if (jsonStr != null && jsonStr.length() > 0) {
                    getTransWeatherInfo(widgetId).strJsonWeatherInfo = jsonStr;
                    mHandler.sendMessage(Message.obtain(null, MSG_DRAW_WIDGET, widgetId, startId));
                }
                else {
                    Log.e("Service", "weather json string is null or zero");
                    stopSelf(startId);
                }
            }
        }).execute();
    }

    private String getSource(String jsonStr) {
        try {
            JSONObject reader = new JSONObject(jsonStr);
            if (reader != null) {
                String source = null;
                if (reader.has("source")) {
                    source = reader.optString("source");
                }

                return source;
            }
        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private GeoInfo getGeoInfo(String jsonStr) {
        try {
            GeoInfo geoInfo = new GeoInfo();
            JSONObject reader = new JSONObject(jsonStr);
            if (reader != null) {
                if (reader.has("name")) {
                    geoInfo.setName(reader.optString("name"));
                }
                if (reader.has("address")) {
                    geoInfo.setAddress(reader.optString("address"));
                }
                if (reader.has("country")) {
                    geoInfo.setCountry(reader.optString("country"));
                }

                return geoInfo;
            }
        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private void updateWidget(int widgetId, final int startId) {
        Log.i("WidgetUpdateService", "update widget="+widgetId);

        TransWeather transWeather = getTransWeatherInfo(widgetId);
        RemoteViews views = null;

        String src = this.getSource(transWeather.strJsonWeatherInfo);
        Log.i("WidgetUpdateService", "source="+src);

        if (transWeather.currentPosition) {
            GeoInfo weatherGeoInfo = this.getGeoInfo(transWeather.strJsonWeatherInfo);
            transWeather.geoInfo.setName(weatherGeoInfo.getName());
            transWeather.geoInfo.setCountry(weatherGeoInfo.getCountry());
            transWeather.geoInfo.setAddress(weatherGeoInfo.getAddress());
            this.saveCurrentGeoInfo(getApplicationContext(), transWeather.geoInfo);
        }

        if (src.equals("KMA")) {
            views = updateKrWeatherWidget(widgetId, transWeather.strJsonWeatherInfo, transWeather.geoInfo.getName());
        }
        else if (src.equals("DSF")) {
            views = updateWorldWeatherWidget(widgetId, transWeather.strJsonWeatherInfo, transWeather.geoInfo.getName());
        }
        else {
            Log.e("WidgetUpdateService", "unknown source="+src);
        }

        if (views != null) {
            mAppWidgetManager.updateAppWidget(widgetId, views);
        }

        if(startId >= 0) {
            Log.i("WidgetUpdateService", "stopSelf startId="+startId);
            stopSelf(startId);
        }
    }

    private RemoteViews updateWorldWeatherWidget(int widgetId, String jsonStr, String locationName) {
        if (jsonStr == null) {
            Log.e("WidgetUpdateService", "jsonData is NULL");
            Toast.makeText(getApplicationContext(), "Fail to get world weather data", Toast.LENGTH_LONG).show();
            return null;
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
            return null;
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
            W2x1WidgetProvider.setWidgetData(context, views, wData);
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
            DailyWeather.setWidgetData(context, views, wData);
            Log.i("UpdateWorldWeather", "set 4x1 daily weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.clock_and_three_days) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            wData.setDayWeather(0, WorldWeatherElement.getDayWeatherFromToday(jsonStr, -1));
            wData.setDayWeather(1, WorldWeatherElement.getDayWeatherFromToday(jsonStr, 0));
            wData.setDayWeather(2, WorldWeatherElement.getDayWeatherFromToday(jsonStr, 1));
            ClockAndThreeDays.setWidgetStyle(context, widgetId, views);
            ClockAndThreeDays.setWidgetData(context, views, wData);
            Log.i("UpdateWorldWeather", "set 4x1 clock and three days id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x2_clock_current_daily) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            //왜 for 전에 미리 한번하지?
            wData.setDayWeather(0, WorldWeatherElement.getDayWeatherFromToday(jsonStr, -1));
            for (int i=0; i<5; i++) {
                wData.setDayWeather(i, WorldWeatherElement.getDayWeatherFromToday(jsonStr, i-1));
            }
            W4x2ClockCurrentDaily.setWidgetStyle(context, widgetId, views);
            W4x2ClockCurrentDaily.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x2 clock current daily id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x1_hourly) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            List<WeatherData> hourly = WorldWeatherElement.getHourlyWeatherFromCurrent(jsonStr);
            int index=0;
            for(WeatherData hourData : hourly){
                wData.setHourlyWeather(index, hourData);
                index++;
            }
            W4x1Hourly.setWidgetStyle(context, widgetId, views);
            W4x1Hourly.setWidgetData(context, views, wData);
            Log.i("UpdateWidgetService", "set 4x1 hourly id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x1_current_hourly) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            List<WeatherData> hourly = WorldWeatherElement.getHourlyWeatherFromCurrent(jsonStr);
            int index=0;
            for(WeatherData hourData : hourly){
                wData.setHourlyWeather(index, hourData);
                index++;
            }
            W4x1Current3Hourly.setWidgetStyle(context, widgetId, views);
            W4x1Current3Hourly.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x1 current hourly id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x2_clock_current_hourly) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            List<WeatherData> hourly = WorldWeatherElement.getHourlyWeatherFromCurrent(jsonStr);
            int index=0;
            for(WeatherData hourData : hourly){
                wData.setHourlyWeather(index, hourData);
                index++;
            }
            W4x2ClockCurrentHourly.setWidgetStyle(context, widgetId, views);
            W4x2ClockCurrentHourly.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x2 clock current hourly id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x3_clock_current_hourly_daily) {
            wData.setCurrentWeather(WorldWeatherElement.getCurrentWeather(jsonStr));
            List<WeatherData> hourly = WorldWeatherElement.getHourlyWeatherFromCurrent(jsonStr);
            int index=0;
            for(WeatherData hourData : hourly){
                wData.setHourlyWeather(index, hourData);
                index++;
            }
            for (int i=0; i<5; i++) {
                wData.setDayWeather(i, WorldWeatherElement.getDayWeatherFromToday(jsonStr, i-1));
            }
            W4x3ClockCurrentHourlyDaily.setWidgetStyle(context, widgetId, views);
            W4x3ClockCurrentHourlyDaily.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x3 clock current hourly daily id=" + widgetId);
        }
        return views;
    }

    /**
     * location listener에서 widgetId가 업데이트됨.
     * @param widgetId
     * @param jsonStr
     */
    private RemoteViews updateKrWeatherWidget(int widgetId, String jsonStr, String locationName) {
        if (jsonStr == null) {
            Log.e("WidgetUpdateService", "jsonData is NULL");
            return null;
        }
        //Log.i("Service", "jsonStr: " + jsonStr);

        // parsing json string to weather class
        WeatherElement weatherElement = WeatherElement.parsingWeatherElementString2Json(jsonStr);
        if (weatherElement == null) {
            Log.e("WidgetUpdateService", "weatherElement is NULL");
            return null;
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
            return null;
        }

        WidgetData wData = null;

        try {
            // make today, yesterday weather info class
            wData = weatherElement.makeWidgetData();
            if (locationName != null) {
                wData.setLoc(locationName);
            }
        }
        catch (Exception e) {
            Log.e("Service", "Exception: " + e.getMessage());
            return null;
        }

        if (wData == null) {
            Log.e("Service", "weather data is null");
            return null;
        }

        Context context = getApplicationContext();
        // input weather content to widget layout

        RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);

        if (mLayoutId == R.layout.w2x1_widget_layout) {
            W2x1WidgetProvider.setWidgetStyle(context, widgetId, views);
            W2x1WidgetProvider.setWidgetData(context, views, wData);
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
            DailyWeather.setWidgetData(context, views, wData);
            Log.i("UpdateWidgetService", "set 4x1 daily weather id=" + widgetId);
        }
        else if (mLayoutId == R.layout.clock_and_three_days) {
            ClockAndThreeDays.setWidgetStyle(context, widgetId, views);
            ClockAndThreeDays.setWidgetData(context, views, wData);
            Log.i("UpdateWidgetService", "set 4x1 clock and three days id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x2_clock_current_daily) {
            W4x2ClockCurrentDaily.setWidgetStyle(context, widgetId, views);
            W4x2ClockCurrentDaily.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x2 clock current daily id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x1_current_hourly) {
            W4x1Current3Hourly.setWidgetStyle(context, widgetId, views);
            W4x1Current3Hourly.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x1 current hourly id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x1_hourly) {
            W4x1Hourly.setWidgetStyle(context, widgetId, views);
            W4x1Hourly.setWidgetData(context, views, wData);
            Log.i("UpdateWidgetService", "set 4x1 hourly id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x2_clock_current_hourly) {
            W4x2ClockCurrentHourly.setWidgetStyle(context, widgetId, views);
            W4x2ClockCurrentHourly.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x2 clock current hourly id=" + widgetId);
        }
        else if (mLayoutId == R.layout.w4x3_clock_current_hourly_daily) {
            W4x3ClockCurrentHourlyDaily.setWidgetStyle(context, widgetId, views);
            W4x3ClockCurrentHourlyDaily.setWidgetData(context, views, wData, mLocalUnits);
            Log.i("UpdateWidgetService", "set 4x3 clock current hourly daily id=" + widgetId);
        }

        return views;
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
        } else if (layoutId == R.layout.w4x2_clock_current_daily) {
            return W4x2ClockCurrentDaily.class;
        } else if (layoutId == R.layout.w4x1_current_hourly) {
            return W4x1Current3Hourly.class;
        } else if (layoutId == R.layout.w4x1_hourly) {
            return W4x1Hourly.class;
        } else if (layoutId == R.layout.w4x2_clock_current_hourly) {
            return W4x2ClockCurrentHourly.class;
        } else if (layoutId == R.layout.w4x3_clock_current_hourly_daily) {
            return W4x3ClockCurrentHourlyDaily.class;
        }

        return null;
    }
}// class end
