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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.bluetooth.PillDfuPresenter;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.bluetooth.PillPeripheral;
import is.hello.sense.bluetooth.exceptions.RssiException;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.FragmentNavigation;
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
    DevicesPresenter devicesPresenter;
    @Inject
    PillDfuPresenter pillDfuPresenter;
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
        if (!(getActivity() instanceof FragmentNavigation)) {
            finishWithResult(Activity.RESULT_CANCELED, null);
            return;
        }
        if (!bluetoothStack.isEnabled()) {
            requestBle();
            return;
        }
        addPresenter(devicesPresenter);
        addPresenter(pillDfuPresenter);
        addPresenter(firmwareCache);
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
                                 setStatus(R.string.connect_pill_fragment_preparing);
                                 pillPeripheral.enterDfuMode(getActivity())
                                               .subscribe(this::pillIsInDFUMode,
                                                          this::presentError);
                             }
                         }, this::presentError);
        devicesPresenter.update();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (diagram != null) {
            diagram.destroy();
        }
        this.diagram = null;
    }

    private void searchForPill() {
        if (isLocationPermissionGranted()) {
            retryButton.post(() -> {
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
            getPillUpdateActivity().flowFinished(this, Activity.RESULT_CANCELED, null);
            return;
        }
        assert sleepPillDevice.firmwareUpdateUrl != null;

        firmwareCache.setUrlLocation(sleepPillDevice.firmwareUpdateUrl);
        final String pillName = sleepPillDevice.getSimpleName();
        pillDfuPresenter.setDesiredPillName(pillName);
        pillDfuPresenter.update();
    }

    private void presentError(@NonNull final Throwable e) {
        retryButton.post(() -> {
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
        ((FragmentNavigation) getActivity()).flowFinished(this, Activity.RESULT_CANCELED, intent);
    }

    private void pillIsInDFUMode(@NonNull final PillPeripheral pillPeripheral) {
        activityStatus.post(() -> getPillUpdateActivity().flowFinished(this, Activity.RESULT_OK, null));
    }

    private void setStatus(@StringRes final int text) {
        activityStatus.post(() -> activityStatus.setText(text));
    }

}
