package is.hello.sense.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.AdvertisingData;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.presenters.ValuePresenter;
import no.nordicsemi.android.dfu.DfuBaseService;
import rx.Observable;

//todo move to commonsense or commonpill??? after this is working

@Singleton
public class PillDfuPresenter extends ValuePresenter<PillPeripheral> {
    public static final String PILL_DFU_NAME = "PillDFU";

    private final Context context;
    private final BluetoothStack bluetoothStack;

    public final PresenterSubject<PillPeripheral> sleepPill = this.subject;
    private String desiredPillName = null;


    @Inject
    public PillDfuPresenter(@NonNull final Context context,
                            @NonNull final BluetoothStack bluetoothStack) {
        this.context = context;
        this.bluetoothStack = bluetoothStack;
    }


    public void setDesiredPillName(@Nullable final String desiredPillName) {
        this.desiredPillName = desiredPillName;
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
        criteria.setDuration(PeripheralCriteria.DEFAULT_DURATION_MS * 2);
        criteria.addPredicate(ad -> (ad.anyRecordMatches(AdvertisingData.TYPE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                                                         b -> Arrays.equals(PillPeripheral.NORMAL_ADVERTISEMENT_SERVICE_128_BIT, b)) ||
                ad.anyRecordMatches(AdvertisingData.TYPE_INCOMPLETE_LIST_OF_128_BIT_SERVICE_CLASS_UUIDS,
                                    b -> Arrays.equals(PillPeripheral.DFU_ADVERTISEMENT_SERVICE_128_BIT, b))));
        return bluetoothStack.discoverPeripherals(criteria)
                             .map(gattPeripherals -> {
                                 Log.e(getClass().getName(), "Desired Pill: " + desiredPillName);
                                 PillPeripheral closestDFUPill = null;
                                 for (final GattPeripheral peripheral : gattPeripherals) {
                                     final String pillName = peripheral.getName();
                                     if (desiredPillName != null && desiredPillName.equals(pillName)) {
                                         return new PillPeripheral(peripheral);
                                     }
                                     if (PILL_DFU_NAME.equals(pillName)) {
                                         if (closestDFUPill == null) {
                                             closestDFUPill = new PillPeripheral(peripheral);
                                         } else if (closestDFUPill.getScanTimeRssi() < peripheral.getScanTimeRssi()) {
                                             closestDFUPill = new PillPeripheral(peripheral);
                                         }
                                     }
                                 }
                                 return closestDFUPill;
                             });

    }


    @Nullable
    @Override
    public Bundle onSaveState() {
        return null;
    }

    //region Pill Interactions

    public void reset() {
        sleepPill.forget();
    }

    public Observable<ComponentName> startDfuService(@NonNull final File file) {
        return Observable.<ComponentName>create(subscriber -> {
            if (sleepPill.getValue() == null) {
                //todo return to first screen. make this a class
                subscriber.onError(new PillNotFoundException());
            }
            String empty = null;
            Intent intent = new Intent(context, DfuService.class);
            intent.putExtra(DfuService.EXTRA_DEVICE_NAME, sleepPill.getValue().getName());
            intent.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, sleepPill.getValue().getAddress());
            intent.putExtra(DfuService.EXTRA_FILE_TYPE, DfuService.TYPE_APPLICATION);
            intent.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
            intent.putExtra(DfuService.EXTRA_KEEP_BOND, false);
            intent.putExtra(DfuService.EXTRA_INIT_FILE_URI, empty);
            intent.putExtra(DfuBaseService.EXTRA_INIT_FILE_PATH, empty);
            intent.putExtra(DfuBaseService.EXTRA_INIT_FILE_RES_ID, 0);
            intent.putExtra(DfuService.EXTRA_FILE_PATH, file.getPath());

            try {
                context.stopService(intent);
                ComponentName componentName = context.startService(intent);
                subscriber.onNext(componentName);
                subscriber.onCompleted();
            } catch (SecurityException e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Rx.mainThreadScheduler());
    }
    //endregion
}
