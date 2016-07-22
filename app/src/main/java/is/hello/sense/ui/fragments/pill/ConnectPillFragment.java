package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.bluetooth.PillPeripheral;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.bluetooth.exceptions.RssiException;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SenseCache;

/**
 * This class requires the Activity it's in implement FragmentNavigation.
 */
public class ConnectPillFragment extends PillHardwareFragment {
    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    SenseCache.FirmwareCache firmwareCache;

    private DiagramVideoView diagram;
    private ProgressBar activityIndicator;
    private TextView activityStatus;
    private Button retryButton;

    //region lifecycle
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getFragmentNavigation() == null) {
            finishWithResult(Activity.RESULT_CANCELED, null);
            return;
        }
        if (!bluetoothStack.isEnabled()) {
            requestBle();
            return;
        }
        //addPresenter(firmwareCache);
        addPresenter(pillDfuPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);
        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);
        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);
        retryButton.setOnClickListener(ignored -> searchForPill());
        this.toolbar = OnboardingToolbar.of(this, view)
                                        .setWantsBackButton(false)
                                        .setOnHelpClickListener(this::help);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(devicesPresenter.devices,
                         this::bindDevices,
                         this::presentError);
        bindAndSubscribe(pillDfuPresenter.sleepPill,
                         pillPeripheral -> {
                             if (pillPeripheral == null) {
                                 presentError(new PillNotFoundException());
                             } else if (pillPeripheral.isTooFar()) {
                                 presentError(new RssiException());
                             } else {
                                 pillFound(pillPeripheral);
                             }
                         }, this::presentError);
        searchForPill();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diagram != null) {
            diagram.destroy();
        }
        this.retryButton.setOnClickListener(null);
        this.retryButton = null;
        this.diagram = null;
    }

    private void searchForPill() {
        if (isLocationPermissionGranted()) {
            retryButton.post(() -> {
                diagram.startPlayback();
                toolbar.setVisible(false);
                activityIndicator.setVisibility(View.VISIBLE);
                activityStatus.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.GONE);
                setStatus(R.string.label_searching_for_pill);
                devicesPresenter.update();
            });
        } else {
            requestLocationPermission();
        }
    }

    private void bindDevices(@NonNull final Devices devices) {
        final SleepPillDevice sleepPillDevice = devices.getSleepPill();
        if (sleepPillDevice == null || !sleepPillDevice.shouldUpdate()) {
            getFragmentNavigation().flowFinished(this, Activity.RESULT_CANCELED, null);
            return;
        }
        assert sleepPillDevice.firmwareUpdateUrl != null;

        firmwareCache.setUrlLocation(sleepPillDevice.firmwareUpdateUrl);
        pillDfuPresenter.update();
    }

    private void presentError(@NonNull final Throwable e) {
        retryButton.post(() -> {
            diagram.suspendPlayback(true);
            toolbar.setVisible(true);
            activityIndicator.setVisibility(View.GONE);
            activityStatus.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);

            @StringRes int title = R.string.error_sleep_pill_title_update_missing;
            @StringRes int message = R.string.error_sleep_pill_message_update_missing;
            final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
            final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity());
            errorDialogBuilder.withOperation(StringRef.from(R.string.connect_pill_fragment_operation).toString());

            if (e instanceof RssiException) {
                title = R.string.error_pill_too_far;
                Analytics.trackEvent(Analytics.PillUpdate.Error.PILL_TOO_FAR, null);
            } else if (e instanceof PillNotFoundException) {
                title = R.string.error_pill_not_found;
                Analytics.trackEvent(Analytics.PillUpdate.Error.PILL_NOT_DETECTED, null);
            } else if (e instanceof ApiException) {
                title = R.string.network_activity_no_connectivity;
                message = R.string.error_network_failure_pair_pill;
            } else {
                errorDialogBuilder.withUnstableBluetoothHelp(getActivity());
            }
            errorDialogBuilder
                    .withTitle(title)
                    .withMessage(StringRef.from(message))
                    .withAction(helpUriString, R.string.label_having_trouble)
                    .build()
                    .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

        });
    }
    //endregion

    @Override
    void onLocationPermissionGranted(final boolean isGranted) {
        if (isGranted) {
            searchForPill();
        }
    }

    private void requestBle() {
        final Intent intent = new Intent();
        intent.putExtra(PillUpdateActivity.ARG_NEEDS_BLUETOOTH, true);
        getFragmentNavigation().flowFinished(this, Activity.RESULT_CANCELED, intent);
    }

    private void pillFound(@NonNull final PillPeripheral pillPeripheral) {
        setStatus(R.string.message_sleep_pill_connected);
        activityStatus.post(() -> activityIndicator.setActivated(true));
        activityStatus.postDelayed(() -> getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, null), 1500);
    }

    private void setStatus(@StringRes final int text) {
        activityStatus.post(() -> activityStatus.setText(text));
    }

}
