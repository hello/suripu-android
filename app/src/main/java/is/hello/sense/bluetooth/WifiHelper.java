package is.hello.sense.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class WifiHelper {
    private final ReplaySubject<Boolean> enabled = ReplaySubject.createWithSize(1);
    private final @Nullable WifiManager wifiManager;

    @Inject WifiHelper(@NonNull Context applicationContext) {
        this.wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            Observable<Intent> stateChanged = fromBroadcast(applicationContext, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            stateChanged.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(ignored -> enabled.onNext(wifiManager.isWifiEnabled()), Functions.LOG_ERROR);
            enabled.onNext(wifiManager.isWifiEnabled());
        } else {
            enabled.onNext(false);
        }
    }

    /**
     * Returns an observable that will continuously report the enabled state of the WiFi radio.
     */
    public Observable<Boolean> isEnabled() {
        return enabled;
    }

    /**
     * Turns on the device's wifi radio.
     */
    Observable<Void> turnOn() {
        if (wifiManager == null || !wifiManager.setWifiEnabled(true)) {
            return Observable.error(new WifiError());
        }

        return enabled.filter(e -> e).map(ignored -> null);
    }

    /**
     * Turns off the device's wifi radio.
     */
    Observable<Void> turnOff() {
        if (wifiManager == null || !wifiManager.setWifiEnabled(false)) {
            return Observable.error(new WifiError());
        }

        return enabled.filter(e -> !e).map(ignored -> null);
    }


    public static class WifiError extends Exception {

    }
}
