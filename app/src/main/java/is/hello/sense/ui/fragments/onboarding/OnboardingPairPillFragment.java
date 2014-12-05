package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheralError;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.util.Analytics;

public class OnboardingPairPillFragment extends InjectionFragment {
    private static final String ARG_COLOR_INDEX = OnboardingPairPillFragment.class.getName() + ".ARG_COLOR_INDEX";

    @Inject HardwarePresenter hardwarePresenter;

    private ProgressBar activityIndicator;
    private TextView activityStatus;
    private Button retryButton;

    private boolean isPairing = false;

    public static OnboardingPairPillFragment newInstance(int colorIndex) {
        OnboardingPairPillFragment fragment = new OnboardingPairPillFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_COLOR_INDEX, colorIndex);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.event(Analytics.EVENT_ONBOARDING_PAIR_PILL, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);
        retryButton.setOnClickListener(ignored -> pairPill());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isPairing) {
            pairPill();
        }
    }

    private void beginPairing() {
        this.isPairing = true;

        activityIndicator.setVisibility(View.VISIBLE);
        activityStatus.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
    }

    private void finishedPairing() {
        hardwarePresenter.clearPeripheral();

        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.showPillInstructions();
    }


    public void pairPill() {
        if (isPairing) {
            return;
        }

        beginPairing();

        if (hardwarePresenter.getPeripheral() == null) {
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> pairPill(), this::presentError);
            return;
        }

        if (!hardwarePresenter.getPeripheral().isConnected()) {
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(hardwarePresenter.getPeripheral()), status -> {
                if (status != HelloPeripheral.ConnectStatus.CONNECTED)
                    return;

                pairPill();
            }, this::presentError);
            return;
        }

        bindAndSubscribe(hardwarePresenter.linkPill(), ignored -> finishedPairing(), this::presentError);
    }

    public void presentError(Throwable e) {
        this.isPairing = false;

        activityIndicator.setVisibility(View.GONE);
        activityStatus.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);

        if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
        } else if (e instanceof OperationTimeoutError || SensePeripheralError.errorTypeEquals(e, MorpheusBle.ErrorType.TIME_OUT)) {
            ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_sleep_pill_scan_timeout));
            dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }
    }
}
