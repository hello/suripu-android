package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.segment.analytics.Properties;

import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.api.model.v2.alerts.AlertDialogViewModel;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseBottomAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

/**
 * Used specifically for displaying {@link Alert} objects in a bottom dialog.
 */
public class BottomAlertDialogFragment extends SenseDialogFragment {

    public static final String TAG = BottomAlertDialogFragment.class.getName() + "TAG";

    private static final String ARG_ALERT = BottomAlertDialogFragment.class.getName() + "ARG_ALERT";

    private AlertDialogViewModel alert;

    public static BottomAlertDialogFragment newInstance(@NonNull final Alert alert, @NonNull final Resources resources) {

        final Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT, new AlertDialogViewModel(alert, resources));
        final BottomAlertDialogFragment fragment = new BottomAlertDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            setAlert((AlertDialogViewModel) getArguments().getSerializable(ARG_ALERT));

            final Properties properties = Analytics.createProperties(
                    Analytics.Timeline.PROP_SYSTEM_ALERT_TYPE, alert.getCategory());

            Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT, properties);
        } else {
            setAlert((AlertDialogViewModel) savedInstanceState.getSerializable(ARG_ALERT));
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final SenseBottomAlertDialog alertDialog = new SenseBottomAlertDialog(getActivity());

        alertDialog.setTitle(alert.getTitle());
        alertDialog.setMessage(alert.getBody());
        alertDialog.setPositiveButton(alert.positiveButtonText, (view, which) -> this.dispatchAlertAction());
        alertDialog.setNeutralButton(alert.neutralButtonText, null);

        return alertDialog;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ALERT, alert);
    }

    private void setAlert(@Nullable final AlertDialogViewModel alert) {
        if(alert == null){
            this.alert = AlertDialogViewModel.NewEmptyInstance(getResources());
            Logger.error(BottomAlertDialogFragment.TAG, " requires non null Alert object passed in arguments");
        } else {
            this.alert = alert;
        }
    }

    private void dispatchAlertAction(){
        switch (alert.getCategory()){
            case SENSE_MUTED:
                if(getActivity() instanceof Alert.ActionHandler){
                    ((Alert.ActionHandler) getActivity()).unMuteSense();
                }
                break;
            case EXPANSION_UNREACHABLE:
            case UNKNOWN:
            default:
                //do nothing
        }
    }
}
