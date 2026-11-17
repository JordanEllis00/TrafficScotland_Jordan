package org.me.gcu.trafficscotland_jordan.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.trafficscotland_jordan.BuildConfig;
import org.me.gcu.trafficscotland_jordan.R;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.parser.DatexXmlParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single source of truth for traffic data.
 * Each feed (unplanned events, roadworks) is resolved independently: live download -> last good cached copy -> bundled sample.
 * All network, file and XML work runs on background executors; results are published with LiveData.postValue,
 * so no hand-written UI-thread hopping or deprecated async helper classes are needed.
 */
public class TrafficRepository {

    private static final String TAG = "TrafficRepository";

    public static final String UNPLANNED_EVENTS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    public static final String CURRENT_ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    /** Refresh interval in minutes (the brief asks for hourly or shorter; 5 min is convenient for demonstration). */
    public static final int AUTO_REFRESH_MINUTES = 5;

    private static final String PREFS_NAME = "traffic_scotland_prefs";
    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_CLIENT_KEY = "client_key";
    private static final String CACHE_UNPLANNED = "cached_unplanned.xml";
    private static final String CACHE_ROADWORKS = "cached_roadworks.xml";

    private enum Source { LIVE, CACHE, SAMPLE }

    /** XML text together with where it came from. */
    private static final class FeedResult {
        final String xml;
        final Source source;
        FeedResult(String xml, Source source) {
            this.xml = xml;
            this.source = source;
        }
    }

    private final Context appContext;
    private final SharedPreferences prefs;

    private final MutableLiveData<List<TrafficSituation>> situationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessageLiveData = new MutableLiveData<>("Initializing...");
    private final MutableLiveData<Boolean> autoRefreshEnabledLiveData = new MutableLiveData<>(true);

    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private ScheduledFuture<?> autoRefreshTask;

    public TrafficRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        startAutoRefresh();
    }

    public LiveData<List<TrafficSituation>> getSituationsLiveData() { return situationsLiveData; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoadingLiveData; }
    public LiveData<String> getStatusMessageLiveData() { return statusMessageLiveData; }
    public LiveData<Boolean> getAutoRefreshEnabledLiveData() { return autoRefreshEnabledLiveData; }

    // ---------------------------------------------------------------- credentials

    public String getClientId() {
        String saved = prefs.getString(KEY_CLIENT_ID, "");
        if (!saved.isEmpty()) return saved;
        return BuildConfig.DATEX_CLIENT_ID != null ? BuildConfig.DATEX_CLIENT_ID : "";
    }

    public String getClientKey() {
        String saved = prefs.getString(KEY_CLIENT_KEY, "");
        if (!saved.isEmpty()) return saved;
        return BuildConfig.DATEX_CLIENT_KEY != null ? BuildConfig.DATEX_CLIENT_KEY : "";
    }

    public void saveCredentials(String clientId, String clientKey) {
        prefs.edit()
                .putString(KEY_CLIENT_ID, clientId != null ? clientId.trim() : "")
                .putString(KEY_CLIENT_KEY, clientKey != null ? clientKey.trim() : "")
                .apply();
    }

    public boolean hasConfiguredCredentials() {
        return !getClientId().isEmpty() && !getClientKey().isEmpty();
    }

    // ---------------------------------------------------------------- scheduling

    public void setAutoRefreshEnabled(boolean enabled) {
        autoRefreshEnabledLiveData.postValue(enabled);
        if (enabled) {
            startAutoRefresh();
        } else if (autoRefreshTask != null) {
            autoRefreshTask.cancel(false);
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshTask != null) {
            autoRefreshTask.cancel(false);
        }
        autoRefreshTask = scheduler.scheduleWithFixedDelay(
                this::refreshTrafficData,
                AUTO_REFRESH_MINUTES,
                AUTO_REFRESH_MINUTES,
                TimeUnit.MINUTES
        );
    }

    /** Stops all background work; called when the owning ViewModel is cleared. */
    public void shutdown() {
        if (autoRefreshTask != null) autoRefreshTask.cancel(false);
        scheduler.shutdownNow();
        networkExecutor.shutdownNow();
    }

    // ---------------------------------------------------------------- refresh

    public void refreshTrafficData() {
        if (!refreshing.compareAndSet(false, true)) {
            return; // a refresh is already running; do not start an overlapping one
        }
        isLoadingLiveData.postValue(true);
        try {
            networkExecutor.execute(this::runRefresh);
        } catch (RuntimeException rejected) {
            refreshing.set(false);
            isLoadingLiveData.postValue(false);
        }
    }

    private void runRefresh() {
        try {
            FeedResult unplanned = loadFeed(UNPLANNED_EVENTS_URL, CACHE_UNPLANNED, R.raw.sample_unplanned);
            FeedResult roadworks = loadFeed(CURRENT_ROADWORKS_URL, CACHE_ROADWORKS, R.raw.sample_roadworks);

            List<TrafficSituation> combined = new ArrayList<>();
            combined.addAll(DatexXmlParser.parseXmlString(unplanned.xml, TrafficSituation.FeedCategory.UNPLANNED_EVENT));
            combined.addAll(DatexXmlParser.parseXmlString(roadworks.xml, TrafficSituation.FeedCategory.CURRENT_ROADWORK));

            String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.UK).format(new Date());
            statusMessageLiveData.postValue(describe(unplanned.source, roadworks.source)
                    + " • " + combined.size() + " situations • Updated " + timeStr);
            situationsLiveData.postValue(combined);
        } catch (RuntimeException e) {
            Log.e(TAG, "Unexpected failure while refreshing traffic data", e);
        } finally {
            isLoadingLiveData.postValue(false);
            refreshing.set(false);
        }
    }

    private static String describe(Source unplanned, Source roadworks) {
        if (unplanned == roadworks) {
            switch (unplanned) {
                case LIVE:   return "Live DATEX II Feed";
                case CACHE:  return "Offline Cached Feed";
                default:     return "Offline / Sample DATEX II Data";
            }
        }
        return "Mixed sources (Incidents: " + label(unplanned) + ", Roadworks: " + label(roadworks) + ")";
    }

    private static String label(Source s) {
        switch (s) {
            case LIVE:  return "live";
            case CACHE: return "cached";
            default:    return "sample";
        }
    }

    /** Live download if possible, otherwise last good cache, otherwise the bundled sample. */
    private FeedResult loadFeed(String url, String cacheName, int sampleResId) {
        if (hasConfiguredCredentials() && isNetworkAvailable()) {
            try {
                String xml = downloadDatexXmlString(url);
                if (DatexXmlParser.isCompleteDocument(xml)) {
                    writeCacheFile(cacheName, xml);   // only a complete document may replace the cache
                    return new FeedResult(xml, Source.LIVE);
                }
                Log.w(TAG, "Live response missing or incomplete for " + url + "; using fallback");
            } catch (Exception e) {
                Log.w(TAG, "Live DATEX II download failed for " + url + ", falling back to cache/sample", e);
            }
        }
        String cached = readCacheFile(cacheName);
        if (!cached.isEmpty()) {
            return new FeedResult(cached, Source.CACHE);
        }
        return new FeedResult(readRawResource(sampleResId), Source.SAMPLE);
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (RuntimeException e) {
            return true; // if the state cannot be read, let the request itself decide
        }
    }

    // ---------------------------------------------------------------- HTTP

    private String downloadDatexXmlString(String endpointUrl) throws Exception {
        String token = Base64.getEncoder().encodeToString(
                (getClientId() + ":" + getClientKey()).getBytes(StandardCharsets.UTF_8));

        String response = executeHttpGet(endpointUrl, "Basic " + token);   // standard HTTP Basic scheme
        if (response == null) {
            response = executeHttpGet(endpointUrl, token);                 // some gateways expect the bare token
        }
        return response;
    }

    private String executeHttpGet(String endpointUrl, String authHeaderValue) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Authorization", authHeaderValue);
            conn.setRequestProperty("Accept", "application/xml, text/xml");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP response code " + code + " for " + endpointUrl);
                return null;
            }
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    // ---------------------------------------------------------------- files

    private void writeCacheFile(String filename, String content) {
        try {
            File file = new File(appContext.getFilesDir(), filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed writing cache file: " + filename, e);
        }
    }

    private String readCacheFile(String filename) {
        try {
            File file = new File(appContext.getFilesDir(), filename);
            if (!file.exists()) return "";
            String text = readAll(new FileInputStream(file));
            return DatexXmlParser.isCompleteDocument(text) ? text : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String readRawResource(int resId) {
        try {
            return readAll(appContext.getResources().openRawResource(resId));
        } catch (Exception e) {
            return "";
        }
    }

    private static String readAll(InputStream in) throws java.io.IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
