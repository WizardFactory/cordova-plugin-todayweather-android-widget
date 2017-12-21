package net.wizardfactory.todayweather.widget.Data;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by aleckim on 2016. 12. 29..
 */

public class Units {
    private String temperatureUnit = null;
    private String windSpeedUnit = null;
    private String pressureUnit = null;
    private String distanceUnit = null;
    private String precipitationUnit = null;
    private String airUnit = null;

    public Units() {
        this.temperatureUnit = "C";
        this.windSpeedUnit = "m/s";
        this.pressureUnit = "hPa";
        this.distanceUnit = "m";
        this.precipitationUnit = "mm";
        this.airUnit = "airkorea";
    }

    public Units(String jsonStr) {
        JSONObject unitObj = null;
        try {
            unitObj = new JSONObject(jsonStr);
            if (unitObj != null) {
                this.temperatureUnit = unitObj.getString("temperatureUnit");
                this.windSpeedUnit = unitObj.getString("windSpeedUnit");
                this.pressureUnit = unitObj.getString("pressureUnit");
                this.distanceUnit = unitObj.getString("distanceUnit");
                this.precipitationUnit = unitObj.getString("precipitationUnit");
                this.airUnit = unitObj.getString("airUnit");
                return;
            }
            Log.e("Units", "Fail to convert string to json");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.temperatureUnit = "C";
        this.windSpeedUnit = "m/s";
        this.pressureUnit = "hPa";
        this.distanceUnit = "m";
        this.precipitationUnit = "mm";
        this.airUnit = "airkorea";
    }

    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public void setWindSpeedUnit(String windSpeedUnit) {
        this.windSpeedUnit = windSpeedUnit;
    }

    public void setPressureUnit(String pressureUnit) {
        this.pressureUnit = pressureUnit;
    }

    public void setDistanceUnit(String distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    public void setPrecipitationUnit(String precipitationUnit) {
        this.precipitationUnit = precipitationUnit;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    public String getWindSpeedUnit() {
        return windSpeedUnit;
    }

    public String getPressureUnit() {
        return pressureUnit;
    }

    public String getDistanceUnit() {
        return distanceUnit;
    }

    public String getPrecipitationUnit() {
        return precipitationUnit;
    }

    public String getAirUnit() {
        return airUnit;
    }
}
