package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class PillDetailsFragment extends DeviceDetailsFragment {
    @Inject DevicesPresenter devicesPresenter;

    //region Lifecycle

    public static PillDetailsFragment newInstance(@NonNull Device device) {
        PillDetailsFragment fragment = new PillDetailsFragment();
        fragment.setArguments(createArguments(device));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_PILL_DETAIL, null);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showActions();
        addDeviceAction(R.string.action_replace_sleep_pill, true, this::unregisterDevice);
        addDeviceAction(R.string.action_replace_battery, true, this::replaceBattery);

        if (device.getState() == Device.State.LOW_BATTERY) {
            showTroubleshootingAlert(R.string.alert_message_low_battery,
                                     R.string.action_replace_battery,
                                     this::replaceBattery);
        } else if (device.isMissing()) {
            String missingMessage = getString(R.string.error_sleep_pill_missing_fmt, device.getLastUpdatedDescription(getActivity()));
            showTroubleshootingAlert(missingMessage,
                                     R.string.action_troubleshoot,
                                     () -> showSupportFor(UserSupport.DeviceIssue.SLEEP_PILL_MISSING));
        } else {
            hideAlert();
        }
    }

    //endregion


    //region Pill Actions

    public void unregisterDevice() {
        Analytics.trackEvent(Analytics.TopView.EVENT_REPLACE_PILL, null);

        SenseAlertDialog alertDialog = new SenseAlertDialog(getActivity());
        alertDialog.setDestructive(true);
        alertDialog.setTitle(R.string.dialog_title_replace_sleep_pill);
        alertDialog.setMessage(R.string.dialog_message_replace_sleep_pill);
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.action_replace_device, (d, which) -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                             ignored -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 finishDeviceReplaced();
                             },
                             e -> {
                                 LoadingDialogFragment.close(getFragmentManager());
                                 ErrorDialogFragment.presentError(getFragmentManager(), e);
                             });
        });
        alertDialog.show();
    }

    public void replaceBattery() {
        Analytics.trackEvent(Analytics.TopView.EVENT_REPLACE_BATTERY, null);

        UserSupport.showReplaceBattery(getActivity());
    }

    //endregion
}
