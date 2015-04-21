package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.view.View;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
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

public class PillDetailsFragment extends DeviceDetailsFragment {
    private static final int REQUEST_CODE_ADVANCED = 0xAd;

    private static final int OPTION_ID_REPLACE_PILL = 0;

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
        addDeviceAction(R.string.action_replace_battery, true, this::replaceBattery);
        addDeviceAction(R.string.title_advanced, false, this::showAdvancedOptions);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADVANCED && resultCode == Activity.RESULT_OK) {
            SenseBottomSheet.Option option = (SenseBottomSheet.Option) data.getSerializableExtra(BottomSheetDialogFragment.RESULT_OPTION);
            switch (option.getOptionId()) {
                case OPTION_ID_REPLACE_PILL: {
                    replaceDevice();
                    break;
                }

                default: {
                    Logger.warn(getClass().getSimpleName(), "Unknown option " + option.getOptionId());
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
        dialog.setDestructive(true);
        dialog.setTitle(R.string.dialog_title_replace_sleep_pill);

        SpannableStringBuilder message = Styles.resolveSupportLinks(getActivity(), getText(R.string.destructive_action_addendum));
        message.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                        ErrorDialogFragment.presentError(getFragmentManager(), e);
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
        advancedOptions.show(getFragmentManager(), BottomSheetDialogFragment.TAG);
    }

    //endregion
}
