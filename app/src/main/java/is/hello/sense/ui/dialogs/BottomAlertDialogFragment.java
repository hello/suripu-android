package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.alerts.Alert;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseBottomAlertDialog;
import is.hello.sense.util.Analytics;

public class BottomAlertDialogFragment extends SenseDialogFragment {

    public static final String TAG = BottomAlertDialogFragment.class.getName() + "TAG";

    private static final String ARG_ALERT = BottomAlertDialogFragment.class.getName() + "ARG_ALERT";

    private Alert alert;

    public static BottomAlertDialogFragment newInstance(@NonNull final Alert alert) {

        final Bundle args = new Bundle();
        args.putSerializable(ARG_ALERT, alert);
        final BottomAlertDialogFragment fragment = new BottomAlertDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.alert = (Alert) getArguments().getSerializable(ARG_ALERT);

        if (savedInstanceState == null) {
            if(alert != null) {
                final Properties properties = Analytics.createProperties(
                        Analytics.Timeline.PROP_SYSTEM_ALERT_TYPE, alert.getCategory());

                Analytics.trackEvent(Analytics.Timeline.EVENT_SYSTEM_ALERT, properties);
            }
        } else {
            this.alert = (Alert) savedInstanceState.getSerializable(ARG_ALERT);
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final SenseBottomAlertDialog alertDialog = new SenseBottomAlertDialog(getActivity());

        alertDialog.setTitle(alert.getTitle());
        alertDialog.setMessage(alert.getBody());

        alertDialog.setPositiveButton(R.string.action_ok, null);

        return alertDialog;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_ALERT, alert);
    }
}
