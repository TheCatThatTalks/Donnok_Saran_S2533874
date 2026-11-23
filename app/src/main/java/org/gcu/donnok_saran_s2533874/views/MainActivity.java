package org.gcu.donnok_saran_s2533874.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import org.gcu.donnok_saran_s2533874.R;
import org.gcu.donnok_saran_s2533874.model.TrafficItem;
import org.gcu.donnok_saran_s2533874.viewmodels.TrafficViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TrafficViewModel viewModel;
    private TrafficAdapter adapter;
    private GoogleMap mMap;
    private RecyclerView recyclerViewTraffic;
    private View mapFragmentContainer;
    private TextView txtDetailPaneTitle, txtDetailPaneBody;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private AlertDialog detailDialog;

    String clientId = "";
    String clientKey = "";
    final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    final String currentRoadworksURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Bundle metaData = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (metaData != null) {
                clientId = metaData.getString("org.gcu.donnok_saran_s2533874.CLIENT_ID");
                clientKey = metaData.getString("org.gcu.donnok_saran_s2533874.CLIENT_KEY");
            }
        } catch (PackageManager.NameNotFoundException ignored) {}

        // Setup RecyclerView Table View
        recyclerViewTraffic = findViewById(R.id.recyclerViewTraffic);
        if (recyclerViewTraffic != null) {
            recyclerViewTraffic.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TrafficAdapter(new ArrayList<>(), this::onTrafficItemClick);
            recyclerViewTraffic.setAdapter(adapter);
        }

        // Setup Map Fragment View
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            if (mapFragment.getView() != null) {
                mapFragmentContainer = mapFragment.getView();
            }
        }

        // Landscape detail pane references (null in portrait)
        txtDetailPaneTitle = findViewById(R.id.txtDetailPaneTitle);
        txtDetailPaneBody = findViewById(R.id.txtDetailPaneBody);

        // View Map Button -> opens MapsActivity overview with filtered items
        Button btnViewMap = findViewById(R.id.btnViewMap);
        if (btnViewMap != null) {
            btnViewMap.setOnClickListener(v -> {
                List<TrafficItem> filtered = viewModel.getFilteredItems().getValue();
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("items", (ArrayList<TrafficItem>) (filtered != null ? filtered : new ArrayList<>()));
                startActivity(intent);
            });
        }


        // Initialize ViewModel BEFORE registering LiveData observers
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        // Observe filtered items and update the Table View adapter
        viewModel.getFilteredItems().observe(this, items -> {
            if (adapter != null && items != null) {
                adapter.updateItems(items);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (loading != null && loading) {
                Toast.makeText(this, "Fetching live Traffic Scotland feeds...", Toast.LENGTH_SHORT).show();
            }
        });

        // Observe selected item to persist selection across orientation changes in both portrait & landscape
        viewModel.getSelectedItem().observe(this, item -> {
            if (item != null) {
                displaySelectedItemUI(item);
            }
        });

        // Initialize Fused Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchUserGpsLocation();
        }

        // Refresh Button
        Button btnStart = findViewById(R.id.startButton);
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> viewModel.startProgress(MainActivity.this, unplannedEventsURL, currentRoadworksURL, clientId, clientKey));
        }

        // Category Filter Buttons
        Button btnAll = findViewById(R.id.btnFilterAll);
        if (btnAll != null) btnAll.setOnClickListener(v -> viewModel.setCategoryFilter("ALL"));

        Button btnRoadworks = findViewById(R.id.btnCurrentRoadworks);
        if (btnRoadworks != null) btnRoadworks.setOnClickListener(v -> viewModel.setCategoryFilter("ROADWORKS"));

        Button btnUnplanned = findViewById(R.id.btnUnplannedRoadworks);
        if (btnUnplanned != null) btnUnplanned.setOnClickListener(v -> viewModel.setCategoryFilter("UNPLANNED"));

        // Road Search Input
        EditText editSearch = findViewById(R.id.editSearchRoad);
        if (editSearch != null) {
            editSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    viewModel.setRoadQuery(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Max Distance Input
        EditText editDist = findViewById(R.id.editMaxDistance);
        if (editDist != null) {
            editDist.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        String input = s.toString().trim();
                        if (input.isEmpty()) {
                            viewModel.setMaxDistanceFilter(-1.0);
                        } else {
                            double km = Double.parseDouble(input);
                            viewModel.setMaxDistanceFilter(km);
                        }
                    } catch (Exception e) {
                        viewModel.setMaxDistanceFilter(-1.0);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        TextView txtFetchStatus = findViewById(R.id.txtFetchStatus);
        viewModel.getFetchStatusMessage().observe(this, statusMsg -> {
            if (statusMsg != null && !statusMsg.isEmpty()) {
                if (txtFetchStatus != null) {
                    txtFetchStatus.setText(statusMsg);
                }
            }
        });

        // Auto fetch on launch
        viewModel.startAutoRefresh(this, unplannedEventsURL, currentRoadworksURL, clientId, clientKey);
    }

    private void onTrafficItemClick(TrafficItem item) {
        handleItemSelection(item);
    }

    private void handleItemSelection(TrafficItem item) {
        if (item != null) {
            viewModel.setSelectedItem(item); // Save selected item in ViewModel
        }
    }

    private void displaySelectedItemUI(TrafficItem item) {
        if (item == null) return;

        if (txtDetailPaneTitle != null && txtDetailPaneBody != null) {
            // Landscape split pane
            txtDetailPaneTitle.setText(item.getTitle());
            txtDetailPaneBody.setText(item.getFormattedDetails(1));

            Button btnOpenMap = findViewById(R.id.btnOpenMap);
            if (btnOpenMap != null) {
                btnOpenMap.setVisibility(View.VISIBLE);
                btnOpenMap.setOnClickListener(v -> openMapsActivity(item));
            }
        } else {
            // Portrait popup dialog
            if (detailDialog != null && detailDialog.isShowing()) {
                detailDialog.dismiss();
            }
            detailDialog = new AlertDialog.Builder(this)
                    .setTitle(item.getTitle())
                    .setMessage(item.getFormattedDetails(1))
                    .setPositiveButton("Close", null)
                    .setNeutralButton("View Full Map", (dialog, which) -> openMapsActivity(item))
                    .show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Marker click listener
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof TrafficItem) {
                TrafficItem item = (TrafficItem) tag;
                handleItemSelection(item);
            } else {
                marker.showInfoWindow();
            }
            return false;
        });

        List<TrafficItem> currentItems = viewModel.getFilteredItems().getValue();
        if (currentItems != null) {
            updateMapMarkers(currentItems);
        }
    }

    private void updateMapMarkers(List<TrafficItem> items) {
        if (mMap == null || items == null) return;

        mMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean[] hasPoints = {false};

        for (TrafficItem item : items) {
            double lat = item.getToLatitude() != 0.0 ? item.getToLatitude() : item.getFromLatitude();
            double lon = item.getToLongitude() != 0.0 ? item.getToLongitude() : item.getFromLongitude();

            if (lat == 0.0 && lon == 0.0) continue;

            LatLng pos = new LatLng(lat, lon);
            boundsBuilder.include(pos);
            hasPoints[0] = true;
            float markerColor = BitmapDescriptorFactory.HUE_RED;
            String severity = item.getDelaySeverity();
            if (severity != null && severity.contains("Short")) {
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
            } else if (severity != null && severity.contains("Medium")) {
                markerColor = BitmapDescriptorFactory.HUE_ORANGE;
            }

            String title = (item.getRoadName() != null && !item.getRoadName().isEmpty() ? "[" + item.getRoadName() + "] " : "") + item.getTitle();

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(title)
                    .snippet("Tap for details | " + severity)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (marker != null) {
                marker.setTag(item);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("Your Location")
                            .snippet("Current GPS Position")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    boundsBuilder.include(userLatLng);
                }

                if (hasPoints[0]){
                try {
                        LatLngBounds bounds = boundsBuilder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                    } catch (Exception ignored) {}
                }
            });
        } else {
            if (hasPoints[0]){
            try {
                    LatLngBounds bounds = boundsBuilder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                } catch (Exception ignored) {}
            }
        }
    }

    private void fetchUserGpsLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null && viewModel != null) {
                    viewModel.setUserLocation(location.getLatitude(), location.getLongitude());
                }
            });
        }
    }

    private void openMapsActivity(TrafficItem item) {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.putExtra("title", item.getTitle());
        intent.putExtra("roadName", item.getRoadName());
        intent.putExtra("toLat", item.getToLatitude());
        intent.putExtra("toLon", item.getToLongitude());
        intent.putExtra("fromLat", item.getFromLatitude());
        intent.putExtra("fromLon", item.getFromLongitude());
        intent.putExtra("severity", item.getDelaySeverity());
        intent.putExtra("category", item.getCategory());
        intent.putExtra("description", item.getDescription());
        intent.putExtra("startTime", item.getStartTime());
        intent.putExtra("endTime", item.getEndTime());
        intent.putExtra("publishedDate", item.getPublishedDate());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
                fetchUserGpsLocation();
            }
        }
    }
}