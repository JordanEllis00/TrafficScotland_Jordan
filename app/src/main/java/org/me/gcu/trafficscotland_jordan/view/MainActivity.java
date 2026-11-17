package org.me.gcu.trafficscotland_jordan.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.trafficscotland_jordan.R;
import org.me.gcu.trafficscotland_jordan.model.SituationFilter;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.viewmodel.TrafficViewModel;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_COARSE_LOCATION = 101;

    private TrafficViewModel viewModel;
    private SituationAdapter adapter;

    private MapView mapView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvStatusBanner;
    private TextView tvEmptyState;
    private TextView tvLocationBadge;

    private MaterialButtonToggleGroup viewModeToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        org.osmdroid.config.Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        );
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        bindViews();
        setupRecyclerView();
        setupFilters();
        setupMap();
        observeViewModel();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerSituations);
        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBar);
        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvLocationBadge = findViewById(R.id.tvLocationBadge);
        viewModeToggle = findViewById(R.id.toggleViewMode);
    }

    private void setupRecyclerView() {
        adapter = new SituationAdapter(situation -> {
            viewModel.selectSituation(situation);
            focusMapOnSituation(situation);
            SituationDetailBottomSheet.newInstance(situation)
                    .show(getSupportFragmentManager(), "situation_detail");
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategory);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipUnplanned) {
                viewModel.setCategoryFilter(SituationFilter.Category.UNPLANNED_ONLY);
            } else if (id == R.id.chipRoadworks) {
                viewModel.setCategoryFilter(SituationFilter.Category.ROADWORKS_ONLY);
            } else {
                viewModel.setCategoryFilter(SituationFilter.Category.ALL);
            }
        });

        EditText etSearchRoad = findViewById(R.id.etSearchRoad);
        etSearchRoad.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setRoadQuery(s != null ? s.toString() : "");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        Spinner spinnerRadius = findViewById(R.id.spinnerDistance);
        String[] radiusLabels = {"Any Distance", "Within 10 km", "Within 25 km", "Within 50 km", "Within 100 km"};
        double[] radiusValues = {-1.0, 10.0, 25.0, 50.0, 100.0};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, radiusLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRadius.setAdapter(spinnerAdapter);
        spinnerRadius.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setMaxDistanceKm(radiusValues[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btnChangeLocation).setOnClickListener(v -> showLocationPickerDialog());

        if (viewModeToggle != null) {
            viewModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                viewModel.setShowMap(checkedId == R.id.btnShowMap);
                if (checkedId == R.id.btnShowMap) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyState.setVisibility(View.GONE);
                    mapView.setVisibility(View.VISIBLE);
                    mapView.onResume();
                } else {
                    mapView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
            // restore the list/map choice after a rotation (the ViewModel survives; the views do not)
            viewModeToggle.check(viewModel.isShowMap() ? R.id.btnShowMap : R.id.btnShowList);
        }
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(7.5);
        mapView.getController().setCenter(new GeoPoint(56.1165, -3.9369));
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getStatusMessage().observe(this, msg ->
                tvStatusBanner.setText(msg));

        viewModel.getReferenceLocationLabel().observe(this, label ->
                tvLocationBadge.setText("Ref: " + label));

        viewModel.getFilteredSituations().observe(this, situations -> {
            adapter.submitList(situations);
            updateMapOverlays(situations);

            boolean isListVisible = (viewModeToggle == null)
                    || (viewModeToggle.getCheckedButtonId() == R.id.btnShowList);
            if (isListVisible && (situations == null || situations.isEmpty())) {
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
            }
        });
    }

    private void updateMapOverlays(List<TrafficSituation> situations) {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        double[] refLoc = viewModel.getReferenceLocation().getValue();
        if (refLoc != null) {
            Marker refMarker = new Marker(mapView);
            refMarker.setPosition(new GeoPoint(refLoc[0], refLoc[1]));
            refMarker.setTitle("Reference Location: " + viewModel.getReferenceLocationLabel().getValue());
            refMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(refMarker);
        }

        if (situations == null) {
            mapView.invalidate();
            return;
        }

        for (TrafficSituation item : situations) {
            if (!item.hasAnyCoordinates()) continue;

            int severityColor = SituationStyle.colorFor(this, item);

            if (item.hasLinearPoints()) {
                GeoPoint startPt = new GeoPoint(item.getFromLatitude(), item.getFromLongitude());
                GeoPoint endPt = new GeoPoint(item.getToLatitude(), item.getToLongitude());

                Polyline line = new Polyline(mapView);
                List<GeoPoint> pts = new ArrayList<>();
                pts.add(startPt);
                pts.add(endPt);
                line.setPoints(pts);
                line.getOutlinePaint().setColor(severityColor);
                line.getOutlinePaint().setStrokeWidth(10f);
                line.setOnClickListener((polyline, mapView1, eventPos) -> {
                    SituationDetailBottomSheet.newInstance(item)
                            .show(getSupportFragmentManager(), "situation_detail");
                    return true;
                });
                mapView.getOverlays().add(line);
            }

            double lat = item.hasDisplayPoint() ? item.getLatitude() : item.getFromLatitude();
            double lon = item.hasDisplayPoint() ? item.getLongitude() : item.getFromLongitude();

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lon));
            marker.setTitle((item.getRoadName().isEmpty() ? "" : item.getRoadName() + " - ") + item.getFormattedType());
            marker.setSnippet(item.getGeneralPublicComment());
            marker.setIcon(ContextCompat.getDrawable(this, SituationStyle.iconFor(item)));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setOnMarkerClickListener((m, mv) -> {
                SituationDetailBottomSheet.newInstance(item)
                        .show(getSupportFragmentManager(), "situation_detail");
                return true;
            });
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    private void focusMapOnSituation(TrafficSituation item) {
        if (mapView == null || !item.hasAnyCoordinates()) return;
        double lat = item.hasDisplayPoint() ? item.getLatitude() : item.getFromLatitude();
        double lon = item.hasDisplayPoint() ? item.getLongitude() : item.getFromLongitude();
        mapView.getController().animateTo(new GeoPoint(lat, lon));
        mapView.getController().setZoom(12.0);
    }

    private void showLocationPickerDialog() {
        String[] options = {
                "Use Phone Coarse Location (GPS/Network)",
                "Glasgow City Centre (55.8642, -4.2518)",
                "Edinburgh City Centre (55.9533, -3.1883)",
                "Stirling (56.1165, -3.9369)",
                "Dundee (56.4620, -2.9707)",
                "Aberdeen (57.1497, -2.0943)",
                "Inverness (57.4778, -4.2247)"
        };
        double[][] coords = {
                {0, 0},
                {55.8642, -4.2518},
                {55.9533, -3.1883},
                {56.1165, -3.9369},
                {56.4620, -2.9707},
                {57.1497, -2.0943},
                {57.4778, -4.2247}
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Reference Location for Distance Search")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        requestCoarseLocation();
                    } else {
                        viewModel.setReferenceLocation(coords[which][0], coords[which][1], options[which]);
                    }
                })
                .show();
    }

    private void requestCoarseLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_COARSE_LOCATION);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (loc != null) {
                viewModel.setReferenceLocation(loc.getLatitude(), loc.getLongitude(),
                        String.format("Device Coarse (%.2f, %.2f)", loc.getLatitude(), loc.getLongitude()));
                Toast.makeText(this, "Updated to device coarse location", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Coarse location unavailable in emulator; keeping Glasgow.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_COARSE_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCoarseLocation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        Boolean autoEnabled = viewModel.getAutoRefreshEnabled().getValue();
        MenuItem autoItem = menu.findItem(R.id.action_auto_refresh);
        if (autoItem != null && autoEnabled != null) {
            autoItem.setChecked(autoEnabled);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            viewModel.refreshData();
            return true;
        } else if (id == R.id.action_auto_refresh) {
            boolean newState = !item.isChecked();
            item.setChecked(newState);
            viewModel.toggleAutoRefresh(newState);
            Toast.makeText(this, newState ? "Auto-refresh every 5 mins ON" : "Auto-refresh OFF", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_credentials) {
            showCredentialsDialog();
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle("About & Data Source")
                    .setMessage("Traffic Scotland Mobile Road Monitor\n\n"
                            + "Data Source: Traffic Scotland DATEX II v2.3 Feed (Transport Scotland).\n\n"
                            + "Duration Colour Legend:\n"
                            + "\u2022 Green: Short Roadworks (1\u20132 days)\n"
                            + "\u2022 Amber: Medium Roadworks (3\u20137 days)\n"
                            + "\u2022 Red: Long Roadworks (> 7 days)\n"
                            + "\u2022 Grey: Roadworks, duration unknown\n"
                            + "\u2022 Blue: Live Unplanned Incident\n\n"
                            + "Icons: accident, closure, congestion, obstruction and roadworks are Google Material Icons "
                            + "(Apache License 2.0). Weather, event and information icons were drawn for this app.\n"
                            + "Map data \u00a9 OpenStreetMap contributors (osmdroid).")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCredentialsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etId = new EditText(this);
        etId.setHint("DATEX II Client ID");
        etId.setText(viewModel.getClientId());
        layout.addView(etId);

        EditText etKey = new EditText(this);
        etKey.setHint("DATEX II Client Key");
        etKey.setText(viewModel.getClientKey());
        layout.addView(etKey);

        new AlertDialog.Builder(this)
                .setTitle("DATEX II API Credentials")
                .setMessage("Configured with your Traffic Scotland DATEX II Client ID and Client Key.")
                .setView(layout)
                .setPositiveButton("Save & Fetch Live", (d, w) ->
                        viewModel.updateCredentials(etId.getText().toString(), etKey.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
