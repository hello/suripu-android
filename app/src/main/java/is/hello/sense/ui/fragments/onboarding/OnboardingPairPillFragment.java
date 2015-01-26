package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheralError;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.OperationTimeoutError;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingPairPillFragment extends HardwareFragment {
    private ProgressBar activityIndicator;
    private TextView activityStatus;
    private Button retryButton;

    private boolean isPairing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_ONBOARDING_PAIR_PILL, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);
        Views.setSafeOnClickListener(retryButton, ignored -> pairPill());

        if (BuildConfig.DEBUG) {
            View diagram = view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);
            diagram.setOnLongClickListener(ignored -> {
                finishedPairing();
                return true;
            });
            diagram.setBackgroundResource(R.drawable.selectable_dark);
        }

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
        LoadingDialogFragment.show(getFragmentManager(), null, true);
        getFragmentManager().executePendingTransactions();
        LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
            if (getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_PAIR_ONLY, false)) {
                hardwarePresenter.clearPeripheral();
                getOnboardingActivity().finish();
            } else {
                getOnboardingActivity().showPillInstructions();
            }
        });
    }


    public void pairPill() {
        beginPairing();

        if (hardwarePresenter.getPeripheral() == null) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.rediscoverLastPeripheral(), ignored -> pairPill(), this::presentError);
            return;
        }

        if (!hardwarePresenter.getPeripheral().isConnected()) {
            showBlockingActivity(R.string.title_scanning_for_sense);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(hardwarePresenter.getPeripheral()), status -> {
                if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                    hideBlockingActivity(true, this::pairPill);
                } else {
                    showBlockingActivity(status.messageRes);
                }
            }, this::presentError);
            return;
        }

        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.linkPill(),
                             ignored -> completeHardwareActivity(this::finishedPairing),
                             this::presentError);
        });
    }

    public void presentError(Throwable e) {
        this.isPairing = false;

        hideAllActivity(false, () -> {
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);

            if (hardwarePresenter.isErrorFatal(e)) {
                UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
                fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
            } else if (e instanceof OperationTimeoutError || SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.TIME_OUT)) {
                MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.error_title_sleep_pill_scan_timeout), getString(R.string.error_message_sleep_pill_scan_timeout));
                messageDialogFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.NETWORK_ERROR)) {
                ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_network_failure_pair_pill));
                dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            } else if (SensePeripheralError.errorTypeEquals(e, SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED)) {
                ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_pill_already_paired));
                dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            } else {
                ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
            }
        });
    }
}
