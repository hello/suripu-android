package is.hello.sense.interactors;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.DeviceOTAState;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

import static is.hello.sense.api.model.DeviceOTAState.OtaState.REQUIRED;
import static is.hello.sense.api.model.DeviceOTAState.OtaState.UNKNOWN;

/**
 * Make call to check if a over the air firmware update is required for Sense device.
 * Stores result through {@link PreferencesInteractor}
 */
@Singleton public class SenseOTAStatusInteractor extends ValueInteractor<DeviceOTAState> {

    public static final String DEVICE_OTA_STATUS = "device_ota_status";

    public InteractorSubject<DeviceOTAState> deviceState = this.subject;

    @Inject
    ApiService apiService;
    @Inject
    PreferencesInteractor preferences;

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<DeviceOTAState> provideUpdateObservable() {
        return apiService.getSenseUpdateStatus();
    }

    public Observable<Void> storeInPrefs(){
        return provideUpdateObservable().map(
                deviceOTAState -> {
                    storeStatus(deviceOTAState);
                    return null;
                });
    }

    public boolean isOTARequired() {
        final DeviceOTAState.OtaState senseOtaStatus = DeviceOTAState.OtaState
                .fromString(preferences.getString(DEVICE_OTA_STATUS, UNKNOWN.name()));
        return REQUIRED.equals(senseOtaStatus);
    }

    public void reset(){
        deviceState.forget();
        storeStatus(null);
    }

    private void storeStatus(@Nullable final DeviceOTAState deviceOTAState){
        final SharedPreferences.Editor editor = preferences.edit();
        if(deviceOTAState == null){
            editor.remove(DEVICE_OTA_STATUS)
                  .apply();
            logEvent("Removed device ota state");
        } else {
            editor.putString(DEVICE_OTA_STATUS, deviceOTAState.state.name())
                  .apply();
            logEvent("Stored device ota state");
        }
    }
}
