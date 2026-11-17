package org.me.gcu.trafficscotland_jordan.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.trafficscotland_jordan.model.SituationFilter;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.repository.TrafficRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel of the MVVM architecture. Holds every piece of UI state (filters, reference location, list/map mode)
 * so that it survives a portrait/landscape change, and exposes the filtered list as LiveData.
 */
public class TrafficViewModel extends AndroidViewModel {

    private static final double DEFAULT_LAT = 55.8642;   // Glasgow City Centre
    private static final double DEFAULT_LON = -4.2518;

    private final TrafficRepository repository;

    private final MutableLiveData<SituationFilter.Category> categoryFilter = new MutableLiveData<>(SituationFilter.Category.ALL);
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Double> maxDistanceKm = new MutableLiveData<>(-1.0);
    private final MutableLiveData<TrafficSituation> selectedSituation = new MutableLiveData<>(null);
    private final MutableLiveData<double[]> referenceLocation = new MutableLiveData<>(new double[]{DEFAULT_LAT, DEFAULT_LON});
    private final MutableLiveData<String> referenceLocationLabel = new MutableLiveData<>("Glasgow (55.86, -4.25)");
    private boolean showMap = false;

    private final MediatorLiveData<List<TrafficSituation>> filteredSituations = new MediatorLiveData<>();

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        filteredSituations.setValue(new ArrayList<>());
        filteredSituations.addSource(repository.getSituationsLiveData(), items -> applyFilters());
        filteredSituations.addSource(categoryFilter, filter -> applyFilters());
        filteredSituations.addSource(roadQuery, query -> applyFilters());
        filteredSituations.addSource(maxDistanceKm, dist -> applyFilters());
        filteredSituations.addSource(referenceLocation, loc -> applyFilters());

        refreshData();   // fetch at start-up; the repository then repeats it on a timer
    }

    public LiveData<List<TrafficSituation>> getFilteredSituations() { return filteredSituations; }
    public LiveData<Boolean> getIsLoading() { return repository.getIsLoadingLiveData(); }
    public LiveData<String> getStatusMessage() { return repository.getStatusMessageLiveData(); }
    public LiveData<Boolean> getAutoRefreshEnabled() { return repository.getAutoRefreshEnabledLiveData(); }
    public LiveData<TrafficSituation> getSelectedSituation() { return selectedSituation; }
    public LiveData<String> getReferenceLocationLabel() { return referenceLocationLabel; }
    public LiveData<double[]> getReferenceLocation() { return referenceLocation; }

    public boolean isShowMap() { return showMap; }
    public void setShowMap(boolean showMap) { this.showMap = showMap; }

    public void refreshData() {
        repository.refreshTrafficData();
    }

    public void setCategoryFilter(SituationFilter.Category filter) {
        categoryFilter.setValue(filter);
    }

    public void setRoadQuery(String query) {
        roadQuery.setValue(query != null ? query : "");
    }

    public void setMaxDistanceKm(double km) {
        maxDistanceKm.setValue(km);
    }

    public void setReferenceLocation(double lat, double lon, String label) {
        referenceLocationLabel.setValue(label);
        referenceLocation.setValue(new double[]{lat, lon});
    }

    public void selectSituation(TrafficSituation situation) {
        selectedSituation.setValue(situation);
    }

    public String getClientId() { return repository.getClientId(); }
    public String getClientKey() { return repository.getClientKey(); }

    public void updateCredentials(String id, String key) {
        repository.saveCredentials(id, key);
        repository.refreshTrafficData();
    }

    public void toggleAutoRefresh(boolean enabled) {
        repository.setAutoRefreshEnabled(enabled);
    }

    private void applyFilters() {
        SituationFilter.Category cat = categoryFilter.getValue() != null ? categoryFilter.getValue() : SituationFilter.Category.ALL;
        String query = roadQuery.getValue() != null ? roadQuery.getValue() : "";
        double maxDist = maxDistanceKm.getValue() != null ? maxDistanceKm.getValue() : -1.0;
        double[] ref = referenceLocation.getValue() != null ? referenceLocation.getValue() : new double[]{DEFAULT_LAT, DEFAULT_LON};

        filteredSituations.setValue(
                SituationFilter.apply(repository.getSituationsLiveData().getValue(), cat, query, maxDist, ref[0], ref[1]));
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }
}
