package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;

import org.json.JSONObject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.widget.SenseBottomAlertDialog;
import is.hello.sense.util.Analytics;

public class DeviceIssueDialogFragment extends SenseDialogFragment {
    public static final String TAG = DeviceIssueDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE_RES = DeviceIssueDialogFragment.class.getName() + ".ARG_TITLE_RES";
    private static final String ARG_MESSAGE_RES = DeviceIssueDialogFragment.class.getName() + ".ARG_MESSAGE_RES";
    private static final String ARG_ACTION_RES = DeviceIssueDialogFragment.class.getName() + ".ARG_ACT_RES";
    private static final String ARG_ISSUE_ORDINAL = DeviceIssueDialogFragment.class.getName() + ".ARG_ISSUE_ORDINAL";


    private DeviceIssuesPresenter.Issue issue;


    //region Lifecycle

    public static DeviceIssueDialogFragment newInstance(@NonNull DeviceIssuesPresenter.Issue issue) {
        if (issue == DeviceIssuesPresenter.Issue.NONE) {
            throw new IllegalArgumentException("Cannot create issue dialog for NONE");
        }

        DeviceIssueDialogFragment dialogFragment = new DeviceIssueDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TITLE_RES, issue.titleRes);
        arguments.putInt(ARG_MESSAGE_RES, issue.messageRes);
        if (issue == DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY) {
            arguments.putInt(ARG_ACTION_RES, R.string.action_replace);
        } else {
            arguments.putInt(ARG_ACTION_RES, R.string.action_fix_now);
        }
        arguments.putInt(ARG_ISSUE_ORDINAL, issue.ordinal());
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int issueOrdinal = getArguments().getInt(ARG_ISSUE_ORDINAL);
        this.issue = DeviceIssuesPresenter.Issue.values()[issueOrdinal];

        if (savedInstanceState == null) {
            Properties properties = Analytics.createProperties(
                Analytics.Timeline.PROP_SYSTEM_ALERT_TYPE, issue.systemAlertType
            );
            Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT, properties);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseBottomAlertDialog alertDialog = new SenseBottomAlertDialog(getActivity());

        Bundle arguments = getArguments();
        alertDialog.setTitle(arguments.getInt(ARG_TITLE_RES));
        alertDialog.setMessage(arguments.getInt(ARG_MESSAGE_RES));

        alertDialog.setNeutralButton(R.string.action_fix_later, (ignored, which) -> dispatchLater());
        alertDialog.setPositiveButton(arguments.getInt(ARG_ACTION_RES), (ignored, which) -> dispatchIssue());

        return alertDialog;
    }

    //endregion


    //region Actions

    private void dispatchLater() {
        Properties properties = Analytics.createProperties(
            Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION, Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION_LATER
        );
        Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT_ACTION, properties);
    }

    private void dispatchIssue() {
        Properties properties = Analytics.createProperties(
            Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION, Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION_NOW
        );
        Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT_ACTION, properties);

        if (issue == DeviceIssuesPresenter.Issue.SLEEP_PILL_LOW_BATTERY) {
            UserSupport.showReplaceBattery(getActivity());
        } else {
            DeviceListFragment.startStandaloneFrom(getActivity());
        }
    }

    //endregion
}
