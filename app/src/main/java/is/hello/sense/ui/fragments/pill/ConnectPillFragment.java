package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
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
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.bluetooth.PillDfuPresenter;
import is.hello.sense.bluetooth.PillPeripheral;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.SenseCache;

public class ConnectPillFragment extends PillHardwareFragment {
    @Inject
    DevicesPresenter devicesPresenter;
    @Inject
    PillDfuPresenter pillDfuPresenter;
    @Inject
    BluetoothStack bluetoothStack;
    @Inject
    SenseCache.FirmwareCache firmwareCache;

    private DiagramVideoView diagram;
    private ProgressBar activityIndicator;
    private TextView activityStatus;
    private Button retryButton;
    private Button skipButton;

    //region lifecycle
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!bluetoothStack.isEnabled()) {
            cancel(true);
            return;
        }
        //addPresenter(pillDfuPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_pair_pill, container, false);
        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_onboarding_pair_pill_activity);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_onboarding_pair_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_retry);
        this.skipButton = (Button) view.findViewById(R.id.fragment_onboarding_pair_pill_skip);
        this.diagram = (DiagramVideoView) view.findViewById(R.id.fragment_onboarding_pair_pill_diagram);

        this.skipButton.setText(R.string.action_cancel);
        Views.setTimeOffsetOnClickListener(retryButton, ignored -> searchForPill());
        Views.setTimeOffsetOnClickListener(skipButton, ignored -> onCancel());
        this.toolbar = OnboardingToolbar.of(this, view)
                                        .setWantsBackButton(false)
                                        .setWantsHelpButton(false)
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
                         this::pillFound,
                         this::presentError);
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
        this.skipButton.setOnClickListener(null);
        this.skipButton = null;
        this.diagram = null;
    }

    @Override
    void onLocationPermissionGranted(final boolean isGranted) {
        if (isGranted) {
            searchForPill();
        }
    }

    private void searchForPill() {
        if (isLocationPermissionGranted()) {
            retryButton.post(() -> {
                updateUI(false);
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
            cancel(false);
            return;
        }
        assert sleepPillDevice.firmwareUpdateUrl != null;

        firmwareCache.setUrlLocation(sleepPillDevice.firmwareUpdateUrl);
        pillDfuPresenter.update();
    }

    private void updateUI(final boolean onError){
        if(onError){
            diagram.suspendPlayback(true);
        } else {
            diagram.startPlayback();
        }
        toolbar.setVisible(onError);
        toolbar.setWantsHelpButton(onError);
        final int visibleOnError = onError ? View.VISIBLE : View.GONE;
        final int hiddenOnError = onError ? View.GONE : View.VISIBLE;
        skipButton.setVisibility(visibleOnError);
        retryButton.setVisibility(visibleOnError);
        activityStatus.setVisibility(hiddenOnError);
        activityIndicator.setVisibility(hiddenOnError);
    }

    private void presentError(@NonNull final Throwable e) {
        retryButton.post(() -> {
            updateUI(true);

            @StringRes final int title = R.string.error_sleep_pill_title_update_missing;
            @StringRes final int message = R.string.error_sleep_pill_message_update_missing;
            final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
            final ErrorDialogFragment.Builder errorDialogBuilder = getErrorDialogFragmentBuilder(e, title, message, helpUriString);
            errorDialogBuilder.withOperation(StringRef.from(R.string.connect_pill_fragment_operation).toString());

            errorDialogBuilder
                    .build()
                    .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

            Log.e(getTag(), "presentError: ", e);

        });
    }
    //endregion

    private void onCancel(){
        pillDfuPresenter.reset();
        cancel(false);
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
