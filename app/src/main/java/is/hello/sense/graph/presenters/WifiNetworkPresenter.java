package is.hello.sense.graph.presenters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.subjects.ReplaySubject;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class WifiNetworkPresenter extends Presenter {
    private final WifiManager wifiManager;
    private final Subscription wifiStateChangedSubscription;

    public final ReplaySubject<List<ScanResult>> networksInRange = ReplaySubject.createWithSize(1);

    @Inject public WifiNetworkPresenter(@NonNull Context context) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Observable<Intent> stateChanged = fromBroadcast(context, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        this.wifiStateChangedSubscription = stateChanged.subscribe(ignored -> update());
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
            networksInRange.onNext(wifiManager.getScanResults());
        } else {
            networksInRange.onError(new WifiScanUnavailable());
        }
    }

    public void showWifiSettingsFrom(@NonNull Activity activity) {
        activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }


    public static class WifiScanUnavailable extends Exception {}
}
