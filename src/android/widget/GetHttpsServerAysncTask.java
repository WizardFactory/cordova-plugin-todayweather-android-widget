package net.wizardfactory.todayweather.widget;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 *  This class http server connect form "GET" Type
 *  Now used for get weather data and location address for geocoder.
 */
public class GetHttpsServerAysncTask extends AsyncTask<String, String, String> {
    private String url = null;
    private AsyncCallback asyncCallback = null;

    public GetHttpsServerAysncTask(String url){
        this.url = url;
    }

    public GetHttpsServerAysncTask(String url, AsyncCallback asyncCallback){
        this.url = url;
        this.asyncCallback = asyncCallback;
    }

    @Override
    protected void onPreExecute() {
        // do nothing
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... arg0) {
        Log.i("AsyncTask" , "url: " + url);
        return GetWeatherFromHttpsServer(url);
    }

    @Override
    protected void onPostExecute(String jsonStr) {
        // do nothing
        if (this.asyncCallback != null) {
            this.asyncCallback.onPostExecute(jsonStr);
        }
    }

    private String GetWeatherFromHttpsServer(String strUrl) {
        String retString = "";
        DataInputStream dis = null;
        StringBuilder messageBuilder = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(30000);
            urlConnection.setConnectTimeout(30000);
            urlConnection.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);

            BufferedReader bi2 = new BufferedReader( new InputStreamReader( urlConnection.getInputStream(), "UTF-8") );
            String s = "";
            while ((s = bi2.readLine()) != null) {
                retString += s;
            }
        } catch (Exception e) {
            Log.e("AsynTask", e.toString());
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        //Log.i("AsyncTask", "ret: " + retString);
        return retString;
    }
}
