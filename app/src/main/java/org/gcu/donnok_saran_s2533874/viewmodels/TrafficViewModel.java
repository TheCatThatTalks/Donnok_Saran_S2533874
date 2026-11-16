package org.gcu.donnok_saran_s2533874.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.gcu.donnok_saran_s2533874.model.TrafficDataFetcher;
import org.gcu.donnok_saran_s2533874.model.TrafficItem;


import java.util.List;

public class TrafficViewModel extends ViewModel{
    private final MutableLiveData<String> rawXmlData = new MutableLiveData<>();
    private final MutableLiveData<List<TrafficItem>> trafficItems = new MutableLiveData<>();

    public LiveData<String> getRawXmlData() {
        return rawXmlData;
    }

    public LiveData<List<TrafficItem>> getTrafficItems() {
        return trafficItems;
    }

    public void startProgress(String url, String clientId, String clientKey) {
        TrafficDataFetcher.fetch(url, clientId, clientKey, (rawXml, items) -> {
            rawXmlData.postValue(rawXml);
            trafficItems.postValue(items);
        });
    }
}
