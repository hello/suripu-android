package is.hello.sense.ui.dialogs;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import com.segment.analytics.Properties;

import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.model.v2.alerts.AlertDialogViewModel;
import is.hello.sense.util.Analytics;

/**
 * Used specifically for displaying {@link Alert} objects in a bottom dialog.
 */
public class SystemAlertDialogFragment extends BottomAlertDialogFragment<AlertDialogViewModel> {

    public static SystemAlertDialogFragment newInstance(@NonNull final Alert alert,
                                                        @NonNull final Resources resources) {

        final Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT, new AlertDialogViewModel(alert, resources));
        final SystemAlertDialogFragment fragment = new SystemAlertDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActionHandler(); //check it exists
    }

    @Override
    AlertDialogViewModel getEmptyDialogViewModelInstance() {
        return AlertDialogViewModel.NewEmptyInstance(getResources());
    }

    @Override
    void onPositiveButtonClicked() {
        dispatchAlertAction();
    }

    private void dispatchAlertAction() {
        final Properties properties = Analytics.createProperties(
                Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION,
                Analytics.Timeline.PROP_EVENT_SYSTEM_ALERT_ACTION_NOW); //todo make more specific
        Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT_ACTION, properties);
        getActionHandler().handleAlert(alert.getAnalyticPropertyType());
    }

    @VisibleForTesting
    @NonNull
    protected Alert.ActionHandler getActionHandler() {
        if (getTargetFragment() instanceof Alert.ActionHandler) {
            return (Alert.ActionHandler) getTargetFragment();
        } else if (getActivity() instanceof Alert.ActionHandler) {
            return (Alert.ActionHandler) getActivity();
        } else {
            throw new IllegalStateException(String.format("%s required to handle actions",
                                                          Alert.ActionHandler.class.getName()));
        }
    }

}
