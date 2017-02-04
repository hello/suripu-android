package is.hello.sense.api.model.v2.alerts;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.util.Constants;

public class DeviceIssueDialogViewModel extends ApiResponse
        implements DialogViewModel<Integer> {

    private final String title;
    private final String body;
    private final String positiveButtonText;
    private final String neutralButtonText;

    private final DeviceIssuesInteractor.Issue issue;

    public static DeviceIssueDialogViewModel createEmptyInstance(@NonNull final Resources resources) {
        return new DeviceIssueDialogViewModel(DeviceIssuesInteractor.Issue.NONE, resources);
    }

    public DeviceIssueDialogViewModel(@NonNull final DeviceIssuesInteractor.Issue issue,
                                      @NonNull final Resources resources) {
        this.issue = issue;
        this.title = getDisplayableString(issue.titleRes, resources);
        this.body = getDisplayableString(issue.messageRes, resources);
        this.positiveButtonText = getDisplayableString(issue.buttonActionRes, resources);
        this.neutralButtonText = resources.getString(R.string.action_fix_later);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String getPositiveButtonText() {
        return positiveButtonText;
    }

    @Override
    public String getNeutralButtonText() {
        return neutralButtonText;
    }

    @Override
    public Integer getAnalyticPropertyType() {
        return issue.systemAlertType;
    }

    public DeviceIssuesInteractor.Issue getIssue() {
        return issue;
    }

    private String getDisplayableString(final int stringResId,
                                        @NonNull final Resources resources) {
        if(stringResId == Constants.NONE) {
            return Constants.EMPTY_STRING;
        } else {
            return resources.getString(stringResId);
        }
    }
}
