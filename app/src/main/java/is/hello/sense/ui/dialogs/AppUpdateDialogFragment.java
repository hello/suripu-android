package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import is.hello.sense.R;
import is.hello.sense.api.model.UpdateCheckIn;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Constants;

public class AppUpdateDialogFragment extends DialogFragment {
    public static final String TAG = AppUpdateDialogFragment.class.getSimpleName();
    private static final String ARG_CHECK_IN_RESPONSE = AppUpdateDialogFragment.class.getSimpleName() + ".ARG_CHECK_IN_RESPONSE";


    private UpdateCheckIn.Response checkInResponse;


    public static AppUpdateDialogFragment newInstance(@NonNull UpdateCheckIn.Response response) {
        AppUpdateDialogFragment fragment = new AppUpdateDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_CHECK_IN_RESPONSE, response);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.checkInResponse = (UpdateCheckIn.Response) getArguments().getSerializable(ARG_CHECK_IN_RESPONSE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

        dialog.setTitle(R.string.dialog_title_new_version);
        if (TextUtils.isEmpty(checkInResponse.getUpdateMessage())) {
            dialog.setMessage(R.string.dialog_message_new_version);
        } else {
            dialog.setMessage(checkInResponse.getUpdateMessage());
        }
        dialog.setPositiveButton(R.string.action_update, (d, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.UPDATE_URL));
            startActivity(intent);
        });
        if (checkInResponse.isUpdateRequired()) {
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            setCancelable(false);
        } else {
            dialog.setNegativeButton(R.string.action_no_thanks, null);
        }

        return dialog;
    }
}
