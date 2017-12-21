package net.wizardfactory.todayweather.widget.JsonElement;

import android.util.Log;

import net.wizardfactory.todayweather.widget.Data.Units;
import net.wizardfactory.todayweather.widget.Data.WeatherData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by aleckim on 2016. 12. 26..
 */
public class WorldWeatherElement {

    private static int _convertCloud2Sky(int cloud) {
        if (cloud <= 0) {
            if (cloud <= 20)  {
                return 1;
            }
            else if (cloud <= 50) {
                return 2;
            }
            else if (cloud <= 80) {
                return 3;
            }
            else {
                return 4;
            }
        }
        else {
            return 1;
        }
    }

    // precType 0: 없음 1:비 2:눈 3:비+눈 4:우박
    // pty 0: nothing, 1 rain 2 rain+snow 3 snow
    private static int _convertPrecType2Pty(int precType) {
        if (precType == 3) {
            return 2;
        }
        else if (precType == 2) {
            return 3;
        }
        return precType;
    }

    private static Date _convertString2Date(String pubdateStr) {
        Date pubDate = null;
        if (pubdateStr != null) {
            SimpleDateFormat transFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
            try {
                pubDate = transFormat.parse(pubdateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return pubDate;
    }

    static private JSONObject _getTheDay(JSONArray daily, Date pubDate) {

        for (int i=0; i<daily.length(); i++) {
            try {
                JSONObject dayInfo;
                Date dayDate;
                dayInfo = daily.getJSONObject(i);
                SimpleDateFormat transFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                dayDate = transFormat.parse(dayInfo.getString("dateObj"));
                if (pubDate.getDate() == dayDate.getDate()) {
                    return dayInfo;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    static public WeatherData getCurrentWeather(String jsonStr) {
        WeatherData currentWeather = new WeatherData();
        try {
            JSONObject reader = new JSONObject(jsonStr);
            if (reader != null) {
                JSONObject timezone = reader.getJSONObject("timezone");
                currentWeather.setTimeZoneOffsetMS(timezone.getInt("ms"));

                JSONArray thisTime = reader.getJSONArray("thisTime");
                JSONObject current = thisTime.getJSONObject(1);
                currentWeather.setTemperature(current.getDouble("t1h"));

                if (current.has("pty")) {
                    currentWeather.setPty(current.getInt("pty"));
                }
                else {
                    currentWeather.setPty(0);
                }
                if (current.has("cloud")) {
                    currentWeather.setSky(_convertCloud2Sky(current.getInt("cloud")));
                }
                else {
                    currentWeather.setSky(0);
                }
                currentWeather.setLgt(0); //not support
                //get pubdate from date of current(thisTime[1])
                currentWeather.setPubDate(_convertString2Date(current.getString("dateObj")));
                currentWeather.setDate(_convertString2Date(current.getString("dateObj")));
                if (current.has("rn1")) {
                    currentWeather.setRn1(current.getDouble("rn1"));
                }
                else {
                    currentWeather.setRn1(0);
                }

                JSONObject todayInfo = _getTheDay(reader.getJSONArray("daily"), currentWeather.getPubDate());
                if (todayInfo != null) {
                    currentWeather.setMaxTemperature(todayInfo.getDouble("tmx"));
                    currentWeather.setMinTemperature(todayInfo.getDouble("tmn"));
                }
                else {
                    Log.e("WorldWeatherElement", "Fail to find today weather info");
                }

                //parsing aqi
                if (current.has("arpltn") && !current.isNull("arpltn")) {
                    JSONObject arpltn = current.getJSONObject("arpltn");
                    //currentWeather.setAqiStationName(arpltn.optString("stationName", null));
                    currentWeather.setAqiStr(arpltn.optString("khaiStr", null));
                    currentWeather.setPm10Str(arpltn.optString("pm10Str", null));
                    currentWeather.setPm25Str(arpltn.optString("pm25Str", null));
                    currentWeather.setAqiValue(arpltn.optInt("khaiValue", WeatherElement.DEFAULT_WEATHER_INT_VAL));
                    currentWeather.setPm10Value(arpltn.optInt("pm10Value", WeatherElement.DEFAULT_WEATHER_INT_VAL));
                    currentWeather.setPm25Value(arpltn.optInt("pm25Value", WeatherElement.DEFAULT_WEATHER_INT_VAL));
                    currentWeather.setAqiGrade(arpltn.optInt("khaiGrade", WeatherElement.DEFAULT_WEATHER_INT_VAL));
                    currentWeather.setPm10Grade(arpltn.optInt("pm10Grade", WeatherElement.DEFAULT_WEATHER_INT_VAL));
                    currentWeather.setPm25Grade(arpltn.optInt("pm25Grade", WeatherElement.DEFAULT_WEATHER_INT_VAL));
                    //currentWeather.setStrAqiPubDate(arpltn.optString("dataTime", null));
                    currentWeather.setAqiPubDate(_convertString2Date(current.getString("dateObj")));
                }
            }
            else {
                Log.e("WorldWeatherElement", "Json string is NULL");
            }
        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }
        return currentWeather;
    }

    static public WeatherData getBefore24hWeather(String jsonStr) {
        WeatherData before24hWeather = new WeatherData();
        try {
            JSONObject reader = new JSONObject(jsonStr);
            if (reader != null) {
                JSONArray thisTime = reader.getJSONArray("thisTime");
                JSONObject before24h = thisTime.getJSONObject(0);
                before24hWeather.setTemperature(before24h.getDouble("t1h"));
                before24hWeather.setPubDate(_convertString2Date(before24h.getString("dateObj")));
                before24hWeather.setDate(_convertString2Date(before24h.getString("dateObj")));
                JSONObject yesterdayInfo = _getTheDay(reader.getJSONArray("daily"), before24hWeather.getPubDate());
                if (yesterdayInfo != null) {
                    before24hWeather.setMaxTemperature(yesterdayInfo.getDouble("tmx"));
                    before24hWeather.setMinTemperature(yesterdayInfo.getDouble("tmn"));
                }
                else {
                    Log.e("WorldWeatherElement", "Fail to find yesterday weather info");
                }
            }
            else {
                Log.e("WorldWeatherElement", "Json string is NULL");
            }
        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }
        return before24hWeather;
    }

    static public List<WeatherData> getHourlyWeatherFromCurrent(String jsonStr) {
        List<WeatherData> hourlyWeather = new ArrayList<WeatherData>();
        try {
            JSONObject reader = new JSONObject(jsonStr);
            if (reader == null) {
                Log.e("WorldWeatherElement", "Json string is NULL");
                return hourlyWeather;
            }

            JSONArray thisTime = reader.getJSONArray("thisTime");
            JSONObject current = thisTime.getJSONObject(1);
            Date theDay = _convertString2Date(current.getString("dateObj"));

            JSONArray hourly = reader.getJSONArray("hourly");

            for (int i=0; i<hourly.length() && hourlyWeather.size() <= 5; i++) {
                JSONObject hourInfo;
                Date dayDate;
                hourInfo = hourly.getJSONObject(i);
                SimpleDateFormat transFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
                dayDate = transFormat.parse(hourInfo.getString("dateObj"));
                if (theDay.getTime() >= dayDate.getTime()) {
                    continue;
                }

                WeatherData hourlyData = new WeatherData();
                hourlyData.setDate(dayDate);
                hourlyData.setTemperature(hourInfo.getDouble("t3h"));
                if (hourInfo.has("pty")) {
                    hourlyData.setPty(hourInfo.getInt("pty"));
                }
                else {
                    hourlyData.setPty(0);
                }
                if (hourInfo.has("cloud")) {
                    hourlyData.setSky(_convertCloud2Sky(hourInfo.getInt("cloud")));
                }
                else {
                    hourlyData.setSky(0);
                }
                hourlyData.setLgt(0); //not support
                if (hourInfo.has("rn1")) {
                    hourlyData.setRn1(hourInfo.getDouble("rn1"));
                }
                else {
                    hourlyData.setRn1(0);
                }
                hourlyWeather.add(hourlyData);
            }

        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
        }

        return hourlyWeather;
    }

    static public WeatherData getDayWeatherFromToday(String jsonStr, int fromToday) {
        WeatherData dayWeather = new WeatherData();
        try {
            JSONObject reader = new JSONObject(jsonStr);
            if (reader != null) {
                JSONArray thisTime = reader.getJSONArray("thisTime");
                JSONObject current = thisTime.getJSONObject(1);
                Date theDay = _convertString2Date(current.getString("dateObj"));
                theDay.setDate(theDay.getDate()+fromToday);

                JSONObject dayInfo = _getTheDay(reader.getJSONArray("daily"), theDay);
                if (dayInfo != null) {
                    dayWeather.setDate(_convertString2Date(dayInfo.getString("dateObj")));
                    dayWeather.setMaxTemperature(dayInfo.getDouble("tmx"));
                    dayWeather.setMinTemperature(dayInfo.getDouble("tmn"));
                    if (dayInfo.has("pty")) {
                        dayWeather.setPty(dayInfo.getInt("pty"));
                    }
                    else {
                        dayWeather.setPty(0);
                    }
                    if (dayInfo.has("cloud")) {
                        dayWeather.setSky(_convertCloud2Sky(dayInfo.getInt("cloud")));
                    }
                    else {
                        dayWeather.setSky(0);
                    }
                    dayWeather.setLgt(0); //not support
                    if (dayInfo.has("rn1")) {
                        dayWeather.setRn1(dayInfo.getDouble("rn1"));
                    }
                    else {
                        dayWeather.setRn1(0);
                    }
                }
                else {
                    Log.e("WorldWeatherElement", "Fail to find yesterday weather info");
                }

            }
            else {
                Log.e("WorldWeatherElement", "Json string is NULL");
            }
        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }

        return dayWeather;
    }

    static public Units getUnits(String jsonStr) {
        try {
            JSONObject reader = new JSONObject(jsonStr);
            if (reader != null) {
                Units units;
                if (reader.has("units")) {
                    units = new Units(reader.optString("units"));
                }
                else {
                    units = new Units();
                }
                return units;
            }
        } catch (JSONException e) {
            Log.e("WorldWeatherElement", "JSONException: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
