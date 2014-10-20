package is.hello.sense.graph.presenters;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hello.ble.devices.Morpheus;

import java.util.Set;

import javax.inject.Inject;

import is.hello.sense.util.BleObserverCallback;
import is.hello.sense.util.Constants;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class DevicePresenter extends Presenter {
    private final PreferencesPresenter preferencesPresenter;

    @Inject public DevicePresenter(@NonNull PreferencesPresenter preferencesPresenter) {
        this.preferencesPresenter = preferencesPresenter;
    }


    public void setPairedDeviceAddress(@Nullable String address) {
        logEvent("saving paired device address: " + address);

        SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (address != null) {
            editor.putString(Constants.GLOBAL_PREF_PAIRED_DEVICE_ADDRESS, address);
        } else {
            editor.remove(Constants.GLOBAL_PREF_PAIRED_DEVICE_ADDRESS);
        }
        editor.apply();
    }

    public Observable<Set<Morpheus>> scanForDevices() {
        logEvent("scanForDevices()");

        return Observable.create((Observable.OnSubscribe<Set<Morpheus>>) s -> Morpheus.discover(new BleObserverCallback<>(s), Constants.BLE_SCAN_TIMEOUT_MS))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public @Nullable Morpheus bestDeviceForPairing(@NonNull Set<Morpheus> devices) {
        // TODO: use RSSI to determine this.
        if (devices.isEmpty())
            return null;
        else
            return devices.iterator().next();
    }

    public Observable<Void> pairWithDevice(@NonNull Morpheus device) {
        logEvent("scanForDevices(" + device + ")");

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.connect(new BleObserverCallback<>(s), true))
                         .doOnNext(ignored -> setPairedDeviceAddress(device.getAddress()))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }
}
