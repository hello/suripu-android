package is.hello.sense.graph.presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class WifiNetworkPresenter extends Presenter {
    public static final String SECURITY_PSK = "PSK";
    public static final String SECURITY_WEP = "WEP";
    public static final String SECURITY_EAP = "EAP";
    public static final String SECURITY_OPEN = "Open";

    private final WifiManager wifiManager;
    private final Subscription wifiStateChangedSubscription;

    public final PresenterSubject<List<ScanResult>> networksInRange = PresenterSubject.create();

    @Inject
    public WifiNetworkPresenter(@NonNull Context context) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        IntentFilter stateChangeFilter = new IntentFilter();
        stateChangeFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        stateChangeFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        Observable<Intent> stateChanged = fromBroadcast(context, stateChangeFilter);
        this.wifiStateChangedSubscription = stateChanged.subscribe(ignored -> update(), Functions.LOG_ERROR);
    }

    @Override
    public void onContainerDestroyed() {
        super.onContainerDestroyed();

        if (!wifiStateChangedSubscription.isUnsubscribed()) {
            wifiStateChangedSubscription.unsubscribe();
        }
    }


    public boolean isWifiNetworkScanningAvailable() {
        return wifiManager.isWifiEnabled() || wifiManager.isScanAlwaysAvailable();
    }

    public void update() {
        if (isWifiNetworkScanningAvailable()) {
            logEvent("update() - processing available networks");
            Observable.OnSubscribe<List<ScanResult>> onSubscribe = s -> {
                List<ScanResult> rawResults = wifiManager.getScanResults();
                if (rawResults == null || rawResults.isEmpty()) {
                    s.onNext(Collections.<ScanResult>emptyList());
                    s.onCompleted();
                    return;
                }

                HashMap<String, ScanResult> results = new HashMap<>();
                for (ScanResult result : rawResults) {
                    if (result.frequency < 2400 || result.frequency > 2499) {
                        continue;
                    }

                    ScanResult preexistingResult = results.get(result.SSID);
                    if (preexistingResult != null && preexistingResult.level > result.level) {
                        continue;
                    }

                    results.put(result.SSID, result);
                }

                List<ScanResult> sortedResults = new ArrayList<>(results.values());
                Collections.sort(sortedResults, (l, r) -> Ints.compare(l.level, r.level));

                s.onNext(sortedResults);
                s.onCompleted();
            };
            Observable.create(onSubscribe)
                      .subscribeOn(Schedulers.io())
                      .subscribe(networksInRange::onNext, networksInRange::onError);
        } else {
            logEvent("update() - networks unavailable");
            networksInRange.onNext(Collections.emptyList());
        }
    }

    public void showWifiSettingsFrom(@NonNull Activity activity) {
        logEvent("showWifiSettingsFrom()");
        activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    public static @NonNull String getSecurityFromCapabilities(@NonNull String capabilities) {
        String[] securityModes = {SECURITY_EAP, SECURITY_PSK, SECURITY_WEP};
        for (String mode : securityModes) {
            if (capabilities.contains(mode))
                return mode;
        }

        return SECURITY_OPEN;
    }
}
