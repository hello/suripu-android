package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.util.Analytics;

public class OnboardingBluetoothFragment extends InjectionFragment {
    @Inject HardwarePresenter hardwarePresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.EVENT_ONBOARDING_NO_BLE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_bluetooth, container, false);

        Button turnOn = (Button) view.findViewById(R.id.fragment_onboarding_bluetooth_turn_on);
        turnOn.setOnClickListener(this::turnOn);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(hardwarePresenter.bluetoothEnabled.filter(Functions.IS_TRUE),
                         ignored -> done(),
                         Functions.LOG_ERROR);
    }

    public void done() {
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(),
                () -> ((OnboardingActivity) getActivity()).showSetupSense());
    }

    public void turnOn(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_turning_on), true);
        bindAndSubscribe(hardwarePresenter.turnOnBluetooth(), ignored -> {}, this::presentError);
    }

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }
    }
}
