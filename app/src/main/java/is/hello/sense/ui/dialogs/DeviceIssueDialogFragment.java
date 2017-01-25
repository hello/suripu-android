package is.hello.sense.ui.dialogs;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;

import is.hello.sense.api.model.v2.alerts.DeviceIssueDialogViewModel;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.util.Analytics;

public class DeviceIssueDialogFragment extends BottomAlertDialogFragment<DeviceIssueDialogViewModel> {

    public static DeviceIssueDialogFragment newInstance(@NonNull final DeviceIssuesInteractor.Issue issue,
                                                        @NonNull final Resources resources) {
        if (issue == DeviceIssuesInteractor.Issue.NONE) {
            throw new IllegalArgumentException("Cannot create issue dialog for NONE");
        }

        final DeviceIssueDialogFragment dialogFragment = new DeviceIssueDialogFragment();
        final Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_ALERT, new DeviceIssueDialogViewModel(issue, resources));
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    DeviceIssueDialogViewModel getEmptyDialogViewModelInstance() {
        return DeviceIssueDialogViewModel.createEmptyInstance(getResources());
    }

    @Override
    void onPositiveButtonClicked() {
        dispatchIssue();
    }

    @Override
    public void onNeutralButtonClicked() {
        super.onNeutralButtonClicked();
        dispatchLater();
    }

    //region Actions

    private void dispatchLater() {
        final Properties properties = Analytics.createProperties(
            Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION, Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION_LATER
        );
        Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT_ACTION, properties);
    }

    private void dispatchIssue() {
        final Properties properties = Analytics.createProperties(
            Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION,
            Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION_NOW);
        Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT_ACTION, properties);
        switch(alert.getIssue()) {
            case SLEEP_PILL_LOW_BATTERY:
                UserSupport.showReplaceBattery(getActivity());
                break;
            case SLEEP_PILL_FIRMWARE_UPDATE_AVAILABLE:
                UserSupport.showUpdatePill(getActivity());
                break;
            default:
                DeviceListFragment.startStandaloneFrom(getActivity());
        }
    }

    //endregion
}
