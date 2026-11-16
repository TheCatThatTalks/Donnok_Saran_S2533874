package org.gcu.donnok_saran_s2533874.model;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TrafficDataFetcher {

    public interface DataCallback {
        void onDataLoaded(String rawXml, List<TrafficItem> items);
    }

    public static void fetch(String myUrl, String clientId, String clientKey, DataCallback callback){
        new Thread(() -> {
            String result = "";
            BufferedReader in = null;
            String inputLine = "";

            try {
                URL url = new URL(myUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                //ADD AUTHORISATION (string with clientId:clientKey)
                urlConnection.setRequestProperty("Authorization", Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));
                try {
                    in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    while ((inputLine = in.readLine()) != null) {
                        result = result + inputLine;
                    }
                    in.close();
                } catch (IOException e) {
                    String error = urlConnection.getErrorStream().toString();
                    Log.e("urlConnection", "ioexception");
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("URL", "ioexception");
            }

            //Clean up any leading garbage characters
            if (result.contains("<?")){
                int i = result.indexOf("<?"); //initial tag
                result = result.substring(i);
            }

            List<TrafficItem> items = new ArrayList<>();

            // Now that you have the xml data into result, you can parse it
            try {
                XmlPullParserFactory factory =
                        XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(result));

                //YOUR PARSING HERE!!!


            } catch (XmlPullParserException e) {
                Log.e("Parsing", "EXCEPTION" + e);
                //throw new RuntimeException(e);
            }

            // Return result back to callback
            callback.onDataLoaded(result, items);
        }).start();
    }

}
