package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
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

import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.bluetooth.DfuService;
import is.hello.sense.bluetooth.PillDfuPresenter;
import is.hello.sense.bluetooth.PillPeripheral;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.SenseCache;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class UpdateReadyPillFragment extends PillHardwareFragment
        implements OnBackPressedInterceptor, DfuProgressListener, PillPeripheral.DfuCallback {

    private final ViewAnimator viewAnimator = new ViewAnimator();
    private SenseAlertDialog backPressedDialog;
    private SenseAlertDialog skipPressedDialog;
    private ProgressBar updateIndicator;
    private TextView activityStatus;
    private Button retryButton;
    private Button skipButton;

    @Inject
    PillDfuPresenter pillDfuPresenter;
    @Inject
    SenseCache.FirmwareCache firmwareCache;

    public static Fragment newInstance() {
        return new UpdateReadyPillFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        skipPressedDialog = new SenseAlertDialog(getActivity());
        skipPressedDialog.setCanceledOnTouchOutside(true);
        skipPressedDialog.setTitle(R.string.dialog_title_skip_update);
        skipPressedDialog.setMessage(R.string.dialog_message_skip_update);
        skipPressedDialog.setPositiveButton(R.string.action_ok, (which, ignored) -> {
            onFinish(false);
        });
        skipPressedDialog.setNegativeButton(android.R.string.cancel, null);

        backPressedDialog = new SenseAlertDialog(getActivity());
        backPressedDialog.setCanceledOnTouchOutside(true);
        backPressedDialog.setTitle(R.string.dialog_title_confirm_leave_app);
        backPressedDialog.setMessage(R.string.dialog_message_confirm_leave_update_pill);
        backPressedDialog.setPositiveButton(R.string.action_ok, (which, ignored) -> {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        });
        backPressedDialog.setNegativeButton(android.R.string.cancel, null);

        //todo remove when release
        bindAndSubscribe(Rx.fromLocalBroadcast(getActivity(), new IntentFilter(DfuService.BROADCAST_LOG)),
                         intent -> Log.d(getClass().getSimpleName(), "broadcast log: " + intent.getStringExtra(DfuService.EXTRA_LOG_MESSAGE)),
                         Functions.IGNORE_ERROR);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pill_update, container, false);
        this.updateIndicator = (ProgressBar) view.findViewById(R.id.fragment_update_pill_progress_determinate);
        this.activityStatus = (TextView) view.findViewById(R.id.fragment_update_pill_status);
        this.retryButton = (Button) view.findViewById(R.id.fragment_update_pill_retry);
        this.skipButton = (Button) view.findViewById(R.id.fragment_update_pill_skip);
        viewAnimator.setAnimatedView(view.findViewById(R.id.blue_box_view));
        activityStatus.setText(R.string.message_sleep_pill_updating);
        Views.setTimeOffsetOnClickListener(skipButton, ignored -> skipPressedDialog.show());
        Views.setTimeOffsetOnClickListener(retryButton, ignored -> connectPill());
        this.toolbar = OnboardingToolbar.of(this, view)
                                        .setOnHelpClickListener(this::help)
                                        .setWantsBackButton(false)
                                        .setWantsHelpButton(false);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DfuServiceListenerHelper.registerProgressListener(getActivity(), this);
        viewAnimator.onViewCreated(getActivity(), R.animator.bluetooth_sleep_pill_ota_animator);
        bindAndSubscribe(pillDfuPresenter.sleepPill,
                         pillPeripheral ->
                                 pillPeripheral.enterDfuMode(getActivity(), this)
                                               .subscribe(ignore -> updatePill(),
                                                          this::presentError),
                         this::presentError);

        bindAndSubscribe(firmwareCache.file
                                 .flatMap(file -> pillDfuPresenter.startDfuService(file)),
                         Functions.NO_OP,
                         this::presentError);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewAnimator.onResume();
    }


    @Override
    public void onPause() {
        viewAnimator.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        DfuServiceListenerHelper.unregisterProgressListener(getActivity(), this);
        super.onDestroyView();

        this.activityStatus = null;
        this.retryButton.setOnClickListener(null);
        this.retryButton = null;
        this.skipPressedDialog = null;
        this.backPressedDialog = null;

        viewAnimator.onDestroyView();
    }


    @Override
    void onLocationPermissionGranted(final boolean isGranted) {
        if (isGranted) {
            connectPill();
        }
    }

    private void connectPill() {
        activityStatus.post(() -> {
            if (isLocationPermissionGranted()) {
                updateUI(false);
                pillDfuPresenter.update();
            } else {
                updateUI(true);
                requestLocationPermission();
            }
        });
    }

    private void updatePill() {
        firmwareCache.update();
    }

    private void updateUI(final boolean onError) {
        activityStatus.post(() -> {
            toolbar.setVisible(onError);
            toolbar.setWantsHelpButton(onError);
            final int visibleOnError = onError ? View.VISIBLE : View.GONE;
            final int hiddenOnError = onError ? View.GONE : View.VISIBLE;
            skipButton.setVisibility(visibleOnError);
            retryButton.setVisibility(visibleOnError);
            activityStatus.setVisibility(hiddenOnError);
            setProgress(0); //reset progress
            updateIndicator.setVisibility(hiddenOnError);
        });
    }

    private void setProgress(final int progress) {
        updateIndicator.post(() -> updateIndicator.setProgress(progress));
    }

    public void presentError(final Throwable e) {
        updateUI(true);

        @StringRes final int title = R.string.error_sleep_pill_title_update_fail;
        @StringRes final int message = R.string.error_sleep_pill_message_update_fail;
        final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_WEAK_RSSI.getUri().toString();
        final ErrorDialogFragment.Builder errorDialogBuilder =
                getErrorDialogFragmentBuilder(e,
                                              title,
                                              message,
                                              helpUriString);
        errorDialogBuilder
                .build()
                .showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);

        Log.e(getTag(), "presentError: ", e);
    }

    private void onFinish(final boolean success) {
        if (!success) {
            pillDfuPresenter.reset();
            getFragmentNavigation().flowFinished(this, Activity.RESULT_CANCELED, null);
            return;
        }

        firmwareCache.trimCache();
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.OPAQUE_BACKGROUND);
            getFragmentManager().executePendingTransactions();

            LoadingDialogFragment.closeWithMessageTransition(
                    getFragmentManager(),
                    () -> {
                        final Intent intent = new Intent()
                                .putExtra(PillUpdateActivity.EXTRA_DEVICE_ID,
                                          pillDfuPresenter.getDeviceId());
                        pillDfuPresenter.reset();
                        getFragmentNavigation().flowFinished(this, Activity.RESULT_OK, intent);
                    },
                    R.string.message_sleep_pill_updated);
        });
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        if (pillDfuPresenter.isUpdating()) {
            backPressedDialog.show();
        } else {
            skipPressedDialog.show();
        }
        return true;
    }

    @Override
    public void onDeviceConnecting(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceConnecting");
    }

    @Override
    public void onDeviceConnected(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceConnected");

    }

    @Override
    public void onDfuProcessStarting(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuProcessStarting");

    }

    @Override
    public void onDfuProcessStarted(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuProcessStarted");
    }

    @Override
    public void onEnablingDfuMode(final String deviceAddress) {
        Log.d("DFU Listener", "onEnablingDfuMode");

    }

    @Override
    public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
        Log.d("DFU Listener", "onProgressChanged " + percent + "%");
        setProgress(50 + (percent / 2));

    }

    @Override
    public void onFirmwareValidating(final String deviceAddress) {
        Log.d("DFU Listener", "onFirmwareValidating");

    }

    @Override
    public void onDeviceDisconnecting(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceDisconnecting");

    }

    @Override
    public void onDeviceDisconnected(final String deviceAddress) {
        Log.d("DFU Listener", "onDeviceDisconnected");

    }

    @Override
    public void onDfuCompleted(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuCompleted");
        onFinish(true);
    }

    @Override
    public void onDfuAborted(final String deviceAddress) {
        Log.d("DFU Listener", "onDfuAborted");

    }

    @Override
    public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
        Log.d("DFU Listener", "onError: " + message + ". errorType: " + errorType + ". error: " + error);
        pillDfuPresenter.setIsUpdating(false);
        presentError(new Throwable(message));
    }

    @Override
    public void onStateChange(final PillPeripheral.PillState state) {
        int value = 0;
        switch (state) {
            case DfuMode:
                value += 10;
            case Wiped:
                value += 10;
            case Connected:
                value += 10;
            case Disconnected:
                value += 10;
            case BondRemoved:
                value += 10;
        }
        setProgress(value);
    }
}
