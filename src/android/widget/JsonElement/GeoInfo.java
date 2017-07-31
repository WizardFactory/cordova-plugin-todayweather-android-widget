package net.wizardfactory.todayweather.widget.JsonElement;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by aleckim on 2016. 12. 29..
 */

public class GeoInfo {
    private String name;
    private String country;
    private String address;
    double lat;
    double lng;
//    double baseLength;

    public GeoInfo() {
//        baseLength = 0.02;
    }

    public void setName(String locationName) {
       this.name = locationName;
    }

    public String getName() {
        return name;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLng() {
        return lng;
    }

    public Boolean setJsonString(String retJson) {

        try {
            JSONObject reader = new JSONObject(retJson);
            if (reader == null) {
                return false;
            }

            String status = reader.getString("status");
            if (!status.equals("OK")) {
                Log.e("Service", "status=" + status);
                return false;
            }

            String sub_level2_types[] = {"political", "sublocality", "sublocality_level_2"};
            String sub_level1_types[] = {"political", "sublocality", "sublocality_level_1"};
            String local_types[] = {"locality", "political"};
            String country_types[] = {"country"};
            String sub_level2_name = null;
            String sub_level1_name = null;
            String local_name = null;
            String country_name = null;

            JSONArray results = reader.getJSONArray("results");
            JSONObject result = null;

            int i;
            for (i = 0; i < results.length(); i++) {
                result = results.getJSONObject(i);
                JSONArray addressComponents = result.getJSONArray("address_components");
                for (int j = 0; j < addressComponents.length(); j++) {
                    JSONObject addressComponent = addressComponents.getJSONObject(j);
                    JSONArray types = addressComponent.getJSONArray("types");
                    if (types.getString(0).equals(sub_level2_types[0])
                            && types.getString(1).equals(sub_level2_types[1])
                            && types.getString(2).equals(sub_level2_types[2])) {
                        sub_level2_name = addressComponent.getString("short_name");
                    }

                    if (types.getString(0).equals(sub_level1_types[0])
                            && types.getString(1).equals(sub_level1_types[1])
                            && types.getString(2).equals(sub_level1_types[2])) {
                        sub_level1_name = addressComponent.getString("short_name");
                    }

                    if (types.getString(0).equals(local_types[0])
                            && types.getString(1).equals(local_types[1])) {
                        local_name = addressComponent.getString("short_name");
                    }

                    if (types.getString(0).equals(country_types[0])) {
                        country_name = addressComponent.getString("short_name");
                    }

                    if (sub_level2_name != null && sub_level1_name != null
                            && local_name != null && country_name != null) {
                        break;
                    }
                }

                if (sub_level2_name != null && sub_level1_name != null
                        && local_name != null && country_name != null) {
                    break;
                }
            }

            String name = null;
            String address = "";
            //국내는 동단위까지 표기해야 함.
            if (country_name != null && country_name.equals("KR")) {
                if (sub_level2_name != null) {
                    address += sub_level2_name;
                    name = sub_level2_name;
                }
            }
            if (sub_level1_name != null) {
                address += " " + sub_level1_name;
                if (name == null) {
                    name = sub_level1_name;
                }
            }
            if (local_name != null) {
                address += " " + local_name;
                if (name == null) {
                    name = local_name;
                }
            }
            if (country_name != null) {
                address += " " + country_name;
                if (name == null) {
                    name = country_name;
                }
            }

            if (name == null || name.equals(country_name)) {
                Log.e("service", "Fail to find location address");
            }
            this.setAddress(address);
            this.setName(name);
            this.setCountry(country_name);

            return true;
        }
        catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 독일어등 소수점표시를 ,로 사용하는 경우가 있어 보임. 주의해야 함.
     * @param val
     * @return
     */
    public double toNormalize(double val) {
        Log.i("GeoInfo", "Val="+val);
        return (double)Math.round(val*1000)/1000;

//        double normal_val;
//        normal_val = val - (val % this.baseLength) + this.baseLength/2;
//        normal_val = Double.parseDouble(String.format("%.2f",normal_val));
//        return normal_val;
    }

    @Override
    public String toString() {
        return "{name:"+this.getName()+", country:"+this.getCountry()+", address:"+
                this.getAddress()+ ", lat:"+this.getLat()+", lng:"+this.getLng()+"}";
    }
}
