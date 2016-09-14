package is.hello.sense.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.bluetooth.exceptions.RssiException;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;
import rx.schedulers.Schedulers;

//todo move to commonsense or commonpill??? after this is working

@Singleton
public class PillDfuInteractor extends ValueInteractor<PillPeripheral> {
    private static final String PILL_DFU_NAME = "PillDFU";
    private static final String PILL_PREFIX = "Pill";
    private static final String EXTRA_DEVICE_ID = PillDfuInteractor.class.getName() + "EXTRA_DEVICE_ID";
    private final Context context;
    private final BluetoothStack bluetoothStack;
    private boolean isUpdating;
    private String deviceId;
    public final InteractorSubject<PillPeripheral> sleepPill = this.subject;


    @Inject
    public PillDfuInteractor(@NonNull final Context context,
                             @NonNull final BluetoothStack bluetoothStack) {
        this.context = context;
        this.bluetoothStack = bluetoothStack;
        this.isUpdating = false;
        this.deviceId = null;
    }

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<PillPeripheral> provideUpdateObservable() {
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.setDuration(PeripheralCriteria.DEFAULT_DURATION_MS);
        criteria.addPredicate(ad -> (PillPeripheral.isPillNormal(ad) || PillPeripheral.isPillDfu(ad)));
        return bluetoothStack.discoverPeripherals(criteria)
                             .map(gattPeripherals -> {
                                 GattPeripheral closestPill = null;
                                 for (final GattPeripheral peripheral : gattPeripherals) {
                                     final String pillName = peripheral.getName();
                                     if (pillName != null && pillName.startsWith(PILL_PREFIX)) {
                                         if (closestPill == null || closestPill.getScanTimeRssi() < peripheral.getScanTimeRssi()) {
                                             closestPill = peripheral;
                                         }
                                     }
                                 }
                                 if (closestPill == null) {
                                     return null;
                                 }
                                 return new PillPeripheral(closestPill);
                             })
                             .flatMap(pillPeripheral -> {
                                 if (pillPeripheral == null) {
                                     return Observable.error(new PillNotFoundException());
                                 } else if (pillPeripheral.isTooFar()) {
                                     return Observable.error(new RssiException());
                                 } else {
                                     return Observable.just(pillPeripheral);
                                 }
                             });

    }


    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle state = new Bundle();
        state.putString(EXTRA_DEVICE_ID, deviceId);
        return state;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        deviceId = savedState.getString(EXTRA_DEVICE_ID);
    }

    //region Pill Interactions

    public boolean isUpdating() {
        return isUpdating;
    }

    public void setIsUpdating(final boolean isUpdating) {
        this.isUpdating = isUpdating;
    }

    public String getDeviceId(){
        return deviceId;
    }

    public void setDeviceId(@Nullable final String deviceId) {
        this.deviceId = deviceId;
    }

    public void reset() {
        isUpdating = false;
        deviceId = null;
        sleepPill.forget();
    }

    public Observable<ComponentName> startDfuService(@NonNull final File file) {
        if (!sleepPill.hasValue()) {
            return Observable.error(new PillNotFoundException());
        }

        if (isUpdating()) {
            logEvent("Dfu service already started.");
            return Observable.empty();
        }


        return Observable.<ComponentName>create(subscriber -> {
            Intent intent = new Intent(context, DfuService.class);
            intent.putExtra(DfuService.EXTRA_DEVICE_NAME, PILL_DFU_NAME);
            intent.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, sleepPill.getValue().getAddress());
            intent.putExtra(DfuService.EXTRA_FILE_TYPE, DfuService.TYPE_APPLICATION);
            intent.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
            intent.putExtra(DfuService.EXTRA_KEEP_BOND, false);
            intent.putExtra(DfuService.EXTRA_FILE_PATH, file.getPath());
            intent.putExtra(DfuService.EXTRA_DISABLE_NOTIFICATION, true);

            try {
                context.stopService(intent);
                ComponentName componentName = context.startService(intent);
                setIsUpdating(true);
                subscriber.onNext(componentName);
                subscriber.onCompleted();
            } catch (SecurityException e) {
                setIsUpdating(false);
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io())
                         .observeOn(Rx.mainThreadScheduler());
    }
    //endregion

}
