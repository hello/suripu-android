package is.hello.sense.ui.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.WifiPowerController;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import rx.Observable;
import rx.functions.Action1;

public class UnstableBluetoothFragment extends InjectionFragment {
    public static final String TAG = UnstableBluetoothFragment.class.getSimpleName();

    @Inject BluetoothStack bluetoothStack;
    @Inject WifiPowerController wifiPowerController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackError("Unstable Bluetooth Stack", -1);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unstable_bluetooth, container, false);

        Button restart = (Button) view.findViewById(R.id.fragment_unstable_bluetooth_restart);
        restart.setOnClickListener(this::restartRadios);

        Button cancel = (Button) view.findViewById(R.id.fragment_unstable_bluetooth_cancel);
        cancel.setOnClickListener(ignored -> dismiss());

        return view;
    }


    public void restartRadios(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager());

        Action1<Throwable> onError = e -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        };

        Observable<Void> turnOff = Observable.combineLatest(bluetoothStack.turnOff(), wifiPowerController.turnOff(), (l, r) -> null);
        bindAndSubscribe(turnOff, ignored -> {
            Observable<Void> turnOn = Observable.combineLatest(bluetoothStack.turnOn(), wifiPowerController.turnOn(), (l, r) -> null);
            bindAndSubscribe(turnOn, ignored1 -> {
                LoadingDialogFragment.close(getFragmentManager());
                dismiss();
            }, onError);
        }, onError);
    }

    public void show(@NonNull FragmentManager fm, @IdRes int containerId) {
        fm.beginTransaction()
                .add(containerId, this, TAG)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(TAG)
                .commit();
    }

    public void dismiss() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
        }
    }
}
