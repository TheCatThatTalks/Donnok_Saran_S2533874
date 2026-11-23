package org.gcu.donnok_saran_s2533874.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.gcu.donnok_saran_s2533874.model.TrafficDataFetcher;
import org.gcu.donnok_saran_s2533874.model.TrafficItem;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//ViewModel that holds UI state, handles filtering, and keeps data alive during screen rotation.
public class TrafficViewModel extends ViewModel {

    //LiveData variables observed by MainActivity
    private final MutableLiveData<List<TrafficItem>> allItems = new MutableLiveData<>();
    private final MutableLiveData<List<TrafficItem>> filteredItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> fetchStatusMessage = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isDataFromCache = new MutableLiveData<>(false);
    private ScheduledExecutorService scheduler;
    private String currentCategory = "ALL";
    private String currentRoadQuery = "";
    private double currentMaxDistanceKm = -1.0;

    //Live User GPS Location (defaults to Glasgow center if GPS is not yet acquired)
    private double userLat = 55.8642;
    private double userLon = -4.2518;

    //LiveData getters
    public LiveData<List<TrafficItem>> getFilteredItems() { return filteredItems; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getFetchStatusMessage() { return fetchStatusMessage; }
    public LiveData<Boolean> getIsDataFromCache() { return isDataFromCache; }
    private final MutableLiveData<TrafficItem> selectedItem = new MutableLiveData<>();
    public LiveData<TrafficItem> getSelectedItem() {return selectedItem;}
    public void setSelectedItem(TrafficItem item) {this.selectedItem.setValue(item);}

    //Update GPS position and refresh active distance filter
    public void setUserLocation(double lat, double lon) {
        this.userLat = lat;
        this.userLon = lon;
        applyFilters(allItems.getValue(), currentCategory, currentRoadQuery, currentMaxDistanceKm);
    }

    //Load both unplanned events & roadworks feeds
    public void startProgress(Context context, String unplannedURL, String roadworksURL, String clientId, String clientKey) {
        isLoading.postValue(true);
        fetchStatusMessage.postValue("Fetching live Traffic Scotland feeds...");

        List<TrafficItem> combined = new ArrayList<>();
        Context appContext = context != null ? context.getApplicationContext() : null;

        //Fetch unplanned incidents first
        TrafficDataFetcher.fetchUnplannedEvents(appContext, unplannedURL, clientId, clientKey, (xml1, items1, isCache1) -> {
            if (items1 != null) combined.addAll(items1);

            //Fetch current roadworks next
            TrafficDataFetcher.fetchCurrentRoadworks(appContext, roadworksURL, clientId, clientKey, (xml2, items2, isCache2) -> {
                if (items2 != null) combined.addAll(items2);
                allItems.postValue(combined);
                isLoading.postValue(false);

                // Determine overall cache status
                boolean overallCache = isCache1 || isCache2;
                isDataFromCache.postValue(overallCache);

                String timeStr = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date());
                String message;

                if (!isCache1 && !isCache2) {
                    message = "Data Source: LIVE (Fetched at " + timeStr + ")";
                } else if (isCache1 && isCache2) {
                    message = "Data Source: CACHE / Offline (Fetched at " + timeStr + ")";
                } else {
                    message = "Data Source: Partial CACHE / LIVE (Fetched at " + timeStr + ")";
                }

                if (combined.isEmpty()) {
                    message += " - No items found";
                } else {
                    message += " (" + combined.size() + " items)";
                }

                fetchStatusMessage.postValue(message);
                applyFilters(combined, currentCategory, currentRoadQuery, currentMaxDistanceKm);
            });
        });
    }

    // Starts initial fetch and schedules repeating auto-refresh every hour.
    public void startAutoRefresh(Context context, String unplannedURL, String roadworksURL, String clientId, String clientKey) {
        // Only fetch if data has not been loaded yet
        if (allItems.getValue() == null || allItems.getValue().isEmpty()) {
            startProgress(context, unplannedURL, roadworksURL, clientId, clientKey);
        }

        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(() -> {
                startProgress(context, unplannedURL, roadworksURL, clientId, clientKey);
            }, 60, 60, TimeUnit.MINUTES); // Refreshes every 60 minutes
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public void setCategoryFilter(String category) {
        this.currentCategory = category;
        applyFilters(allItems.getValue(), currentCategory, currentRoadQuery, currentMaxDistanceKm);
    }

    public void setRoadQuery(String query) {
        this.currentRoadQuery = query;
        applyFilters(allItems.getValue(), currentCategory, currentRoadQuery, currentMaxDistanceKm);
    }

    public void setMaxDistanceFilter(double maxKm) {
        this.currentMaxDistanceKm = maxKm;
        applyFilters(allItems.getValue(), currentCategory, currentRoadQuery, currentMaxDistanceKm);
    }

    // Filter pipeline combining category, text search, and GPS distance
    private void applyFilters(List<TrafficItem> sourceList, String category, String roadQuery, double maxKm) {
        if (sourceList == null) return;
        List<TrafficItem> result = new ArrayList<>();

        for (TrafficItem item : sourceList) {
            boolean matchesCategory = "ALL".equals(category)
                    || ("ROADWORKS".equals(category) && item.isMaintenanceWork())
                    || ("UNPLANNED".equals(category) && !item.isMaintenanceWork());

            boolean matchesRoad = roadQuery == null || roadQuery.trim().isEmpty()
                    || item.getRoadName().toLowerCase().contains(roadQuery.toLowerCase().trim())
                    || item.getDescription().toLowerCase().contains(roadQuery.toLowerCase().trim());

            // maxKm < 0.0 means distance filtering is OFF (all items match)
            boolean matchesDistance = maxKm < 0.0 || item.calculateDistanceKm(userLat, userLon) <= maxKm;

            if (matchesCategory && matchesRoad && matchesDistance) {
                result.add(item);
            }
        }
        filteredItems.postValue(result);

        // Check status message AFTER the loop finishes
        if (maxKm >= 0.0) {
            String distStr = (maxKm == (long) maxKm) ? String.valueOf((long) maxKm) : String.valueOf(maxKm);
            if (result.isEmpty()) {
                fetchStatusMessage.postValue("No incidents found within " + distStr + " km.");
            } else {
                fetchStatusMessage.postValue(result.size() + " incident(s) found within " + distStr + " km.");
            }
        }
    }
}