package is.hello.sense.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class WifiPowerController {
    private final Context applicationContext;
    private final @Nullable WifiManager wifiManager;

    private boolean hadConnection = true;

    @Inject WifiPowerController(@NonNull Context applicationContext) {
        this.wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        this.applicationContext = applicationContext;
    }

    /**
     * Returns a new observable that will run a preflight task, and if that task returns true,
     * will subscribe to the broadcast matching a given intent name, sending a signal and
     * unsubscribing when the given matcher function returns true for a received intent.
     * @param intentAction  The name of the intent action to observe.
     * @param task          The task to run before creating a broadcast receiver.
     * @param intentMatcher A function to apply to each received broadcast, used to determine when the observation is completed.
     * @return  An main thread observable.
     */
    private Observable<Void> doTaskAndObserveBroadcast(@NonNull String intentAction,
                                                       @NonNull Func1<Subscriber<? super Void>, Boolean> task,
                                                       @NonNull Func1<Intent, Boolean> intentMatcher) {
        return Observable.create((Observable.OnSubscribe<Void>) s -> {
            if (task.call(s)) {
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intentMatcher.call(intent)) {
                            s.onNext(null);
                            s.onCompleted();

                            applicationContext.unregisterReceiver(this);
                        }
                    }
                };
                applicationContext.registerReceiver(receiver, new IntentFilter(intentAction));
            }
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Turns on the device's wifi radio.
     */
    public Observable<Void> turnOn() {
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            return Observable.just(null);
        }

        if (hadConnection) {
            return doTaskAndObserveBroadcast(WifiManager.NETWORK_STATE_CHANGED_ACTION, subscriber -> {
                if (wifiManager == null || !wifiManager.setWifiEnabled(true)) {
                    subscriber.onError(new SetWifiEnabledFailedError());
                    return false;
                } else {
                    return true;
                }
            }, intent -> {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                return (info != null && info.isConnected());
            });
        } else {
            return doTaskAndObserveBroadcast(WifiManager.WIFI_STATE_CHANGED_ACTION, subscriber -> {
                if (wifiManager == null || !wifiManager.setWifiEnabled(true)) {
                    subscriber.onError(new SetWifiEnabledFailedError());
                    return false;
                } else {
                    return true;
                }
            }, intent -> {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                return (wifiState == WifiManager.WIFI_STATE_ENABLED);
            });
        }
    }

    /**
     * Turns off the device's wifi radio.
     */
    public Observable<Void> turnOff() {
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            return Observable.just(null);
        }

        return doTaskAndObserveBroadcast(WifiManager.WIFI_STATE_CHANGED_ACTION, subscriber -> {
            if (wifiManager == null || !wifiManager.setWifiEnabled(false)) {
                subscriber.onError(new SetWifiEnabledFailedError());
                return false;
            } else {
                this.hadConnection = (wifiManager.getConnectionInfo() != null);
                return true;
            }
        }, intent -> {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            return (wifiState == WifiManager.WIFI_STATE_DISABLED);
        });
    }


    public static class SetWifiEnabledFailedError extends Exception {}
}
