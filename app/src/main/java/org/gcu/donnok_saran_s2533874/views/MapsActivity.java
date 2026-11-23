package org.gcu.donnok_saran_s2533874.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import org.gcu.donnok_saran_s2533874.R;
import org.gcu.donnok_saran_s2533874.model.TrafficItem;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private double toLat, toLon, fromLat, fromLon;
    private String title, roadName, category, severity, description, startTime, endTime, publishedDate;
    private List<TrafficItem> trafficItems;
    private TrafficItem selectedItem;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView txtRoadTitle, txtSeverity, txtCategory, txtTimes, txtDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        if (getIntent() != null) {
            if (getIntent().hasExtra("items")) {
                trafficItems = (List<TrafficItem>) getIntent().getSerializableExtra("items");
            }
            title = getIntent().getStringExtra("title");
            roadName = getIntent().getStringExtra("roadName");
            toLat = getIntent().getDoubleExtra("toLat", 0.0);
            toLon = getIntent().getDoubleExtra("toLon", 0.0);
            fromLat = getIntent().getDoubleExtra("fromLat", 0.0);
            fromLon = getIntent().getDoubleExtra("fromLon", 0.0);
            severity = getIntent().getStringExtra("severity");
            category = getIntent().getStringExtra("category");
            description = getIntent().getStringExtra("description");
            startTime = getIntent().getStringExtra("startTime");
            endTime = getIntent().getStringExtra("endTime");
            publishedDate = getIntent().getStringExtra("publishedDate");
        }

        txtRoadTitle = findViewById(R.id.mapTxtRoadTitle);
        txtSeverity = findViewById(R.id.mapTxtSeverity);
        txtCategory = findViewById(R.id.mapTxtCategory);
        txtTimes = findViewById(R.id.mapTxtTimes);
        txtDescription = findViewById(R.id.mapTxtDescription);

        if (trafficItems == null || trafficItems.isEmpty()) {
            updateBottomCard(title, roadName, severity, category, startTime, endTime, description, publishedDate);
        }

        androidx.cardview.widget.CardView infoCard = findViewById(R.id.mapInfoCard);
        if (infoCard != null) {
            infoCard.setOnClickListener(v -> {
                if (trafficItems != null && !trafficItems.isEmpty() && selectedItem != null) {
                    // Open single incident map view for the currently selected item on the bottom card
                    Intent intent = new Intent(MapsActivity.this, MapsActivity.class);
                    intent.putExtra("title", selectedItem.getTitle());
                    intent.putExtra("roadName", selectedItem.getRoadName());
                    intent.putExtra("toLat", selectedItem.getToLatitude());
                    intent.putExtra("toLon", selectedItem.getToLongitude());
                    intent.putExtra("fromLat", selectedItem.getFromLatitude());
                    intent.putExtra("fromLon", selectedItem.getFromLongitude());
                    intent.putExtra("severity", selectedItem.getDelaySeverity());
                    intent.putExtra("category", selectedItem.getCategory());
                    intent.putExtra("description", selectedItem.getDescription());
                    intent.putExtra("startTime", selectedItem.getStartTime());
                    intent.putExtra("endTime", selectedItem.getEndTime());
                    intent.putExtra("publishedDate", selectedItem.getPublishedDate());
                    startActivity(intent);
                } else {
                    // Single item mode popup dialog fallback
                    String dialogTitle = (roadName != null && !roadName.isEmpty() ? "[" + roadName + "] " : "") + (title != null ? title : "Traffic Event");
                    String formattedStart = TrafficItem.formatDate(startTime);
                    String formattedEnd = TrafficItem.formatDate(endTime);
                    String formattedPublished = TrafficItem.formatDate(publishedDate);

                    StringBuilder sb = new StringBuilder();
                    sb.append("Category: ").append(category != null ? category : "").append("\n");
                    if (roadName != null && !roadName.isEmpty()) {
                        sb.append("Road: ").append(roadName).append("\n");
                    }
                    sb.append("Severity: ").append(severity != null ? severity : "").append("\n");
                    if (formattedStart != null && !formattedStart.isEmpty()) {
                        sb.append("Start Time: ").append(formattedStart).append("\n");
                    }
                    if (formattedEnd != null && !formattedEnd.isEmpty()) {
                        sb.append("End Time: ").append(formattedEnd).append("\n");
                    }
                    if (formattedPublished != null && !formattedPublished.isEmpty()) {
                        sb.append("Published: ").append(formattedPublished).append("\n");
                    }
                    sb.append("\nDescription:\n").append(description != null && !description.isEmpty() ? description : "No additional description available.");

                    new androidx.appcompat.app.AlertDialog.Builder(MapsActivity.this)
                            .setTitle(dialogTitle)
                            .setMessage(sb.toString())
                            .setPositiveButton("Close", null)
                            .show();
                }
            });
        }

        android.widget.Button btnOpenSingleMap = findViewById(R.id.btnOpenSingleMap);
        if (btnOpenSingleMap != null) {
            if (trafficItems != null && !trafficItems.isEmpty()) {
                btnOpenSingleMap.setVisibility(android.view.View.VISIBLE);
            } else {
                btnOpenSingleMap.setVisibility(android.view.View.GONE);
            }
            btnOpenSingleMap.setOnClickListener(v -> {
                if (trafficItems != null && !trafficItems.isEmpty() && selectedItem != null) {
                    Intent intent = new Intent(MapsActivity.this, MapsActivity.class);
                    intent.putExtra("title", selectedItem.getTitle());
                    intent.putExtra("roadName", selectedItem.getRoadName());
                    intent.putExtra("toLat", selectedItem.getToLatitude());
                    intent.putExtra("toLon", selectedItem.getToLongitude());
                    intent.putExtra("fromLat", selectedItem.getFromLatitude());
                    intent.putExtra("fromLon", selectedItem.getFromLongitude());
                    intent.putExtra("severity", selectedItem.getDelaySeverity());
                    intent.putExtra("category", selectedItem.getCategory());
                    intent.putExtra("description", selectedItem.getDescription());
                    intent.putExtra("startTime", selectedItem.getStartTime());
                    intent.putExtra("endTime", selectedItem.getEndTime());
                    intent.putExtra("publishedDate", selectedItem.getPublishedDate());
                    startActivity(intent);
                }
            });
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void updateBottomCard(String pTitle, String pRoadName, String pSeverity, String pCategory, String pStartTime, String pEndTime, String pDescription, String pPublished) {
        // Keep reference variables updated when clicking different markers in list mode
        this.title = pTitle;
        this.roadName = pRoadName;
        this.severity = pSeverity;
        this.category = pCategory;
        this.startTime = pStartTime;
        this.endTime = pEndTime;
        this.description = pDescription;
        this.publishedDate = pPublished;

        if (txtRoadTitle != null) {
            String displayTitle = (roadName != null && !roadName.isEmpty() ? "[" + roadName + "] " : "") + (title != null ? title : "Traffic Event");
            txtRoadTitle.setText(displayTitle);
        }
        if (txtSeverity != null) {
            txtSeverity.setText(severity != null ? severity : "Event");
            try {
                if (severity != null && severity.contains("Short")) {
                    txtSeverity.setBackgroundColor(Color.parseColor("#2E7D32"));
                } else if (severity != null && severity.contains("Medium")) {
                    txtSeverity.setBackgroundColor(Color.parseColor("#EF6C00"));
                } else {
                    txtSeverity.setBackgroundColor(Color.parseColor("#C62828"));
                }
            } catch (Exception ignored) {}
        }
        if (txtCategory != null) {
            txtCategory.setText("Category: " + (category != null ? category : "Roadworks"));
        }
        if (txtTimes != null) {
            StringBuilder timesStr = new StringBuilder();
            String formattedStart = TrafficItem.formatDate(startTime);
            String formattedEnd = TrafficItem.formatDate(endTime);
            if (formattedStart != null && !formattedStart.isEmpty()) timesStr.append("Start: ").append(formattedStart);
            if (formattedEnd != null && !formattedEnd.isEmpty()) {
                if (timesStr.length() > 0) timesStr.append(" | ");
                timesStr.append("End: ").append(formattedEnd);
            }
            if (timesStr.length() == 0) timesStr.append("Live Traffic Event");
            txtTimes.setText(timesStr.toString());
        }
        if (txtDescription != null) {
            txtDescription.setText(description != null && !description.isEmpty() ? description : "No additional description available.");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        setupMapContent();
    }

    private void setupMapContent() {
        if (mMap == null) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean[] hasPoints = {false};

        if (trafficItems != null && !trafficItems.isEmpty()) {
            // MULTI-ITEM OVERVIEW MODE
            for (TrafficItem item : trafficItems) {
                double lat = item.getToLatitude() != 0.0 ? item.getToLatitude() : item.getFromLatitude();
                double lon = item.getToLongitude() != 0.0 ? item.getToLongitude() : item.getFromLongitude();

                if (lat == 0.0 && lon == 0.0) continue;

                LatLng pos = new LatLng(lat, lon);
                boundsBuilder.include(pos);
                hasPoints[0] = true;

                float markerColor = BitmapDescriptorFactory.HUE_RED;
                String itemSev = item.getDelaySeverity();
                if (itemSev != null && itemSev.contains("Short")) {
                    markerColor = BitmapDescriptorFactory.HUE_GREEN;
                } else if (itemSev != null && itemSev.contains("Medium")) {
                    markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                }

                String markerTitle = (item.getRoadName() != null && !item.getRoadName().isEmpty() ? "[" + item.getRoadName() + "] " : "") + item.getTitle();

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(markerTitle)
                        .snippet("view details below")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                if (marker != null) {
                    marker.setTag(item);
                }
            }

            // Marker click listener updates bottom card and tracks selectedItem
            mMap.setOnMarkerClickListener(marker -> {
                Object tag = marker.getTag();
                if (tag instanceof TrafficItem) {
                    TrafficItem item = (TrafficItem) tag;
                    selectedItem = item; // <-- Track selected item
                    updateBottomCard(item.getTitle(), item.getRoadName(), item.getDelaySeverity(), item.getCategory(), item.getStartTime(), item.getEndTime(), item.getDescription(), item.getPublishedDate());
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return false;
            });

            // Set initial card to first item
            if (!trafficItems.isEmpty()) {
                TrafficItem first = trafficItems.get(0);
                selectedItem = first; // <-- Track initial item
                updateBottomCard(first.getTitle(), first.getRoadName(), first.getDelaySeverity(), first.getCategory(), first.getStartTime(), first.getEndTime(), first.getDescription(), first.getPublishedDate());
            }

        } else {
            // SINGLE ITEM MODE (Existing)
            double lat = toLat != 0.0 ? toLat : fromLat;
            double lon = toLon != 0.0 ? toLon : fromLon;

            if (lat == 0.0 && lon == 0.0) return;

            LatLng targetPos = new LatLng(lat, lon);
            boundsBuilder.include(targetPos);
            hasPoints[0] = true;

            float markerColor = BitmapDescriptorFactory.HUE_RED;
            if (severity != null && severity.contains("Short")) {
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
            } else if (severity != null && severity.contains("Medium")) {
                markerColor = BitmapDescriptorFactory.HUE_ORANGE;
            }

            mMap.addMarker(new MarkerOptions()
                    .position(targetPos)
                    .title((roadName != null && !roadName.isEmpty() ? "[" + roadName + "] " : "") + title)
                    .snippet("Severity: " + severity)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (fromLat != 0.0 && fromLon != 0.0 && toLat != 0.0 && toLon != 0.0
                    && (fromLat != toLat || fromLon != toLon)) {
                LatLng fromPos = new LatLng(fromLat, fromLon);
                boundsBuilder.include(fromPos);
                mMap.addMarker(new MarkerOptions()
                        .position(fromPos)
                        .title("Start Point: " + roadName)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                mMap.addPolyline(new PolylineOptions()
                        .add(fromPos, targetPos)
                        .width(10)
                        .color(Color.RED));
            }
        }

        // Location & Camera Framing
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("Your Location")
                            .snippet("Current GPS Position")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    boundsBuilder.include(userLatLng);
                    hasPoints[0] = true;
                }

                if (hasPoints[0]) {
                    try {
                        LatLngBounds bounds = boundsBuilder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                    } catch (Exception ignored) {}
                }
            });
        } else {
            if (hasPoints[0]) {
                try {
                    LatLngBounds bounds = boundsBuilder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
                if (mMap != null) {
                    setupMapContent();
                }
            }
        }
    }
}