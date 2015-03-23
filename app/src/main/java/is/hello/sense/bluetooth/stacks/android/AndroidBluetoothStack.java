package is.hello.sense.bluetooth.stacks.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.EnumSet;
import java.util.List;

import is.hello.sense.bluetooth.errors.BluetoothDisabledError;
import is.hello.sense.bluetooth.errors.BluetoothGattError;
import is.hello.sense.bluetooth.errors.BluetoothPowerChangeError;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.bluetooth.errors.PeripheralBondAlterationError;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.subjects.ReplaySubject;

public class AndroidBluetoothStack implements BluetoothStack {
    final @NonNull Context applicationContext;
    final @NonNull Scheduler scheduler;

    final @NonNull BluetoothManager bluetoothManager;
    private final @Nullable BluetoothAdapter adapter;

    private final @NonNull ReplaySubject<Boolean> enabled = ReplaySubject.createWithSize(1);

    public AndroidBluetoothStack(@NonNull Context applicationContext, @NonNull Scheduler scheduler) {
        this.applicationContext = applicationContext;
        this.scheduler = scheduler;

        this.bluetoothManager = (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = bluetoothManager.getAdapter();
        if (adapter != null) {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (newState == BluetoothAdapter.STATE_ON) {
                        enabled.onNext(true);
                    } else if (newState == BluetoothAdapter.STATE_OFF || newState == BluetoothAdapter.ERROR) {
                        enabled.onNext(false);
                    }
                }
            };
            applicationContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            enabled.onNext(adapter.isEnabled());
        } else {
            Logger.warn(LOG_TAG, "Host device has no bluetooth hardware!");
            enabled.onNext(false);
        }
    }


    @NonNull BluetoothAdapter getAdapter() {
        if (adapter == null) {
            throw new NullPointerException("Host device has no bluetooth hardware!");
        }

        return adapter;
    }


    private Observable<List<Peripheral>> createLeScanner(@NonNull PeripheralCriteria peripheralCriteria) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return newConfiguredObservable(new LollipopLePeripheralScanner(this, peripheralCriteria));
        } else {
            return newConfiguredObservable(new LegacyLePeripheralScanner(this, peripheralCriteria));
        }
    }

    @NonNull
    @Override
    public Observable<List<Peripheral>> discoverPeripherals(@NonNull PeripheralCriteria peripheralCriteria) {
        if (adapter != null && adapter.isEnabled()) {
            if (peripheralCriteria.wantsHighPowerPreScan) {
                Observable<List<BluetoothDevice>> devices = newConfiguredObservable(new HighPowerPeripheralScanner(this, false));
                return devices.flatMap(ignoredDevices -> {
                    Logger.info(LOG_TAG, "High power pre-scan completed.");
                    return createLeScanner(peripheralCriteria);
                });
            } else {
                return createLeScanner(peripheralCriteria);
            }
        } else {
            return Observable.error(new BluetoothDisabledError());
        }
    }

    @NonNull
    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public <T> Observable<T> newConfiguredObservable(Observable.OnSubscribe<T> onSubscribe) {
        return Observable.create(onSubscribe).subscribeOn(getScheduler());
    }

    @Override
    public Observable<Boolean> isEnabled() {
        return this.enabled;
    }

    @Override
    public Observable<Void> turnOn() {
        if (adapter == null) {
            return Observable.error(new BluetoothPowerChangeError());
        }

        ReplaySubject<Void> turnOnMirror = ReplaySubject.createWithSize(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (oldState == BluetoothAdapter.STATE_OFF && newState == BluetoothAdapter.STATE_TURNING_ON) {
                    Logger.info(LOG_TAG, "Bluetooth turning on");
                } else if (oldState == BluetoothAdapter.STATE_TURNING_ON && newState == BluetoothAdapter.STATE_ON) {
                    Logger.info(LOG_TAG, "Bluetooth turned on");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onNext(null);
                    turnOnMirror.onCompleted();
                } else {
                    Logger.info(LOG_TAG, "Bluetooth failed to turn on");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onError(new BluetoothPowerChangeError());
                }
            }
        };
        applicationContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (!adapter.enable()) {
            applicationContext.unregisterReceiver(receiver);
            return Observable.error(new BluetoothPowerChangeError());
        }

        return turnOnMirror;
    }

    @Override
    public Observable<Void> turnOff() {
        if (adapter == null) {
            return Observable.error(new BluetoothPowerChangeError());
        }

        ReplaySubject<Void> turnOnMirror = ReplaySubject.createWithSize(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (oldState == BluetoothAdapter.STATE_ON && newState == BluetoothAdapter.STATE_TURNING_OFF) {
                    Logger.info(LOG_TAG, "Bluetooth turning off");
                } else if (oldState == BluetoothAdapter.STATE_TURNING_OFF && newState == BluetoothAdapter.STATE_OFF) {
                    Logger.info(LOG_TAG, "Bluetooth turned off");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onNext(null);
                    turnOnMirror.onCompleted();
                } else {
                    Logger.info(LOG_TAG, "Bluetooth failed to turn off");

                    applicationContext.unregisterReceiver(this);

                    turnOnMirror.onError(new BluetoothPowerChangeError());
                }
            }
        };
        applicationContext.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (!adapter.disable()) {
            applicationContext.unregisterReceiver(receiver);
            return Observable.error(new BluetoothPowerChangeError());
        }

        return turnOnMirror;
    }

    @Override
    public boolean errorRequiresReconnect(@Nullable Throwable e) {
        return (e != null && (e instanceof OperationTimeoutError ||
                e instanceof BluetoothGattError ||
                e instanceof PeripheralBondAlterationError));
    }

    @Override
    public EnumSet<Traits> getTraits() {
        return EnumSet.of(Traits.BONDS_NOT_PERSISTENT);
    }

    @Override
    public SupportLevel getDeviceSupportLevel() {
        return DeviceSupport.getDeviceSupportLevel();
    }


    @Override
    public String toString() {
        return "AndroidBluetoothStack{" +
                "applicationContext=" + applicationContext +
                ", scheduler=" + scheduler +
                ", adapter=" + adapter +
                ", traits=" + getTraits() +
                '}';
    }
}
