package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.view.View;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.BottomSheetDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.SenseBottomSheet;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class PillDetailsFragment extends DeviceDetailsFragment<SleepPillDevice> {
    private static final int REQUEST_CODE_ADVANCED = 0xAd;

    private static final int OPTION_ID_REPLACE_PILL = 0;

    @Inject DevicesPresenter devicesPresenter;

    //region Lifecycle

    public static PillDetailsFragment newInstance(@NonNull SleepPillDevice device) {
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
        addDeviceAction(R.drawable.icon_settings_battery, R.string.action_replace_battery, this::replaceBattery);
        addDeviceAction(R.drawable.icon_settings_advanced, R.string.title_advanced, this::showAdvancedOptions);

        if (device.state == SleepPillDevice.State.LOW_BATTERY) {
            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(R.string.issue_message_low_battery))
                    .setPrimaryButtonTitle(R.string.action_replace_battery)
                    .setPrimaryButtonOnClick(this::replaceBattery);
            showTroubleshootingAlert(alert);
        } else if (device.isMissing()) {
            final String missingMessage = getString(R.string.error_sleep_pill_missing_fmt, device.getLastUpdatedDescription(getActivity()));
            final TroubleshootingAlert alert = new TroubleshootingAlert()
                    .setMessage(StringRef.from(missingMessage))
                    .setPrimaryButtonTitle(R.string.action_troubleshoot)
                    .setPrimaryButtonOnClick(() -> showSupportFor(UserSupport.DeviceIssue.SLEEP_PILL_MISSING));
            showTroubleshootingAlert(alert);
        } else {
            hideAlert();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADVANCED && resultCode == Activity.RESULT_OK) {
            int optionId = data.getIntExtra(BottomSheetDialogFragment.RESULT_OPTION_ID, 0);
            switch (optionId) {
                case OPTION_ID_REPLACE_PILL: {
                    replaceDevice();
                    break;
                }

                default: {
                    Logger.warn(getClass().getSimpleName(), "Unknown option " + optionId);
                    break;
                }
            }
        }
    }

    //endregion


    //region Pill Actions

    public void replaceDevice() {
        Analytics.trackEvent(Analytics.TopView.EVENT_REPLACE_PILL, null);

        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        dialog.setTitle(R.string.dialog_title_replace_sleep_pill);

        SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
        message.insert(0, getString(R.string.dialog_message_replace_sleep_pill));
        dialog.setMessage(message);

        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(R.string.action_replace_device, (d, which) -> {
            LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(devicesPresenter.unregisterDevice(device),
                    ignored -> {
                        LoadingDialogFragment.close(getFragmentManager());
                        finishDeviceReplaced();
                    },
                    e -> {
                        LoadingDialogFragment.close(getFragmentManager());
                        ErrorDialogFragment.presentError(getActivity(), e);
                    });
        });
        dialog.show();
    }

    public void replaceBattery() {
        Analytics.trackEvent(Analytics.TopView.EVENT_REPLACE_BATTERY, null);

        UserSupport.showReplaceBattery(getActivity());
    }

    public void showAdvancedOptions() {
        Analytics.trackEvent(Analytics.TopView.EVENT_PILL_ADVANCED, null);

        ArrayList<SenseBottomSheet.Option> options = new ArrayList<>();
        options.add(
            new SenseBottomSheet.Option(OPTION_ID_REPLACE_PILL)
                    .setTitle(R.string.action_replace_sleep_pill)
                    .setTitleColor(getResources().getColor(R.color.light_accent))
                    .setDescription(R.string.description_replace_sleep_pill)
        );
        BottomSheetDialogFragment advancedOptions = BottomSheetDialogFragment.newInstance(R.string.title_advanced, options);
        advancedOptions.setTargetFragment(this, REQUEST_CODE_ADVANCED);
        advancedOptions.showAllowingStateLoss(getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    //endregion
}
