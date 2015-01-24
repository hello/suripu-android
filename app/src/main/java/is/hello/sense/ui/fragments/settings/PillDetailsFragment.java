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
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Logger;

public class PillDetailsFragment extends DeviceDetailsFragment {
    @Inject DevicesPresenter devicesPresenter;

    //region Lifecycle

    public static PillDetailsFragment newInstance(@NonNull Device device) {
        PillDetailsFragment fragment = new PillDetailsFragment();
        fragment.setArguments(createArguments(device));
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showActions();
        addDeviceAction(R.string.action_replace_sleep_pill, true, this::unregisterDevice);

        if (device.isMissing()) {
            String missingMessage = getString(R.string.error_sleep_pill_missing_fmt, device.getLastUpdatedDescription(getActivity()));
            showTroubleshootingAlert(missingMessage, R.string.action_troubleshoot, () -> showSupportFor(UserSupport.DeviceIssue.SLEEP_PILL_MISSING));
        } else {
            hideAlert();
        }
    }

    //endregion


    //region Pill Actions

    public void unregisterDevice() {
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

    //endregion
}
