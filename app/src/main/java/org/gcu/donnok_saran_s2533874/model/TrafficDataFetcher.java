package org.gcu.donnok_saran_s2533874.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;

//Fetches XML feeds from traffic Scotland in the background
//If the inter is down, it will load from cache
public class TrafficDataFetcher {

    private static final String PREF_NAME = "TrafficCachePrefs";

    //Callback to pass parsed data back to the ViewModel/Activity
    public interface DataCallback {
        void onDataLoaded(String rawXml, List<TrafficItem> items, boolean isFromCache);
    }

    //Quick helper to grab unplanned incident events
    public static void fetchUnplannedEvents(Context context, String url, String clientId, String clientKey, DataCallback callback) {
        fetch(context, url, "unplanned_cache", clientId, clientKey, callback);
    }

    // Quick helper to grab current roadworks
    public static void fetchCurrentRoadworks(Context context, String url, String clientId, String clientKey, DataCallback callback) {
        fetch(context, url, "roadworks_cache", clientId, clientKey, callback);
    }

    //Main fetch logic running on a background thread
    public static void fetch(Context context, String myUrl, String cacheKey, String clientId, String clientKey, DataCallback callback) {
        new Thread(() -> {
            String result = "";
            boolean success = false;
            boolean isFromCache = false;

            try {
                URL url = new URL(myUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(8000); //wait up to 8 sec to connect
                urlConnection.setReadTimeout(8000); //wait up to 8 sec to read

                //Pass basic auth credentials in HTTP header
                urlConnection.setRequestProperty("Authorization", Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                int responseCode = urlConnection.getResponseCode();
                Log.d("HTTP_STATUS", "URL: " + myUrl + " -> Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine).append("\n");
                    }
                    result = sb.toString();
                    in.close();
                    success = true;
                    isFromCache = false; // Successfully fetched from network

                    // Clean up leading garbage characters
                    if (result.contains("<?")) {
                        int i = result.indexOf("<?");
                        result = result.substring(i);
                    }

                    //Save XML data locally so app works offline next time
                    if (context != null) {
                        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                        prefs.edit().putString(cacheKey, result).apply();
                    }
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e("TrafficDataFetcher", "Network fetch failed, loading from cache...", e);
            }

            //If network failed or offline, load from SharedPreferences cache
            //Fallback to cache if network fails
            if (!success && context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                result = prefs.getString(cacheKey, "");
                isFromCache = true; // Data came from cache
            }
            //Parse XML Data using TrafficXmlParser
            List<TrafficItem> items = TrafficXmlParser.parse(result);

            //Return result back to callback
            callback.onDataLoaded(result, items, isFromCache);
        }).start();
    }
}