package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.widget.SenseBottomAlertDialog;

public class DeviceIssueDialogFragment extends SenseDialogFragment {
    public static final String TAG = DeviceIssueDialogFragment.class.getSimpleName();

    private static final String ARG_TITLE_RES = DeviceIssueDialogFragment.class.getName() + ".ARG_TITLE_RES";
    private static final String ARG_MESSAGE_RES = DeviceIssueDialogFragment.class.getName() + ".ARG_MESSAGE_RES";
    private static final String ARG_ACTION_RES = DeviceIssueDialogFragment.class.getName() + ".ARG_ACT_RES";
    private static final String ARG_ISSUE_ORDINAL = DeviceIssueDialogFragment.class.getName() + ".ARG_ISSUE_ORDINAL";


    //region Lifecycle

    public static DeviceIssueDialogFragment newInstance(@NonNull DevicesPresenter.Issue issue) {
        if (issue == DevicesPresenter.Issue.NONE) {
            throw new IllegalArgumentException("Cannot create issue dialog for NONE");
        }

        DeviceIssueDialogFragment dialogFragment = new DeviceIssueDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TITLE_RES, issue.titleRes);
        arguments.putInt(ARG_MESSAGE_RES, issue.messageRes);
        if (issue == DevicesPresenter.Issue.SLEEP_PILL_LOW_BATTERY) {
            arguments.putInt(ARG_ACTION_RES, R.string.action_replace);
        } else {
            arguments.putInt(ARG_ACTION_RES, R.string.action_fix_now);
        }
        arguments.putInt(ARG_ISSUE_ORDINAL, issue.ordinal());
        dialogFragment.setArguments(arguments);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseBottomAlertDialog alertDialog = new SenseBottomAlertDialog(getActivity());

        Bundle arguments = getArguments();
        alertDialog.setTitle(arguments.getInt(ARG_TITLE_RES));
        alertDialog.setMessage(arguments.getInt(ARG_MESSAGE_RES));

        alertDialog.setNeutralButton(R.string.action_fix_later, null);
        alertDialog.setPositiveButton(arguments.getInt(ARG_ACTION_RES), (ignored, which) -> dispatchIssue());

        return alertDialog;
    }

    //endregion


    //region Actions

    private void showDevices() {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(R.string.label_devices), DeviceListFragment.class, null);
        Intent intent = new Intent(getActivity(), FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }

    private void dispatchIssue() {
        int issueOrdinal = getArguments().getInt(ARG_ISSUE_ORDINAL);
        DevicesPresenter.Issue issue = DevicesPresenter.Issue.values()[issueOrdinal];
        if (issue == DevicesPresenter.Issue.SLEEP_PILL_LOW_BATTERY) {
            UserSupport.showReplaceBattery(getActivity());
        } else {
            showDevices();
        }
        dismissSafely();
    }

    //endregion
}
