package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.R;

public class LoadingDialogFragment extends DialogFragment {
    public static final String TAG = LoadingDialogFragment.class.getSimpleName();

    public static void show(@NonNull FragmentManager fm) {
        LoadingDialogFragment dialog = new LoadingDialogFragment();
        dialog.show(fm, TAG);
    }

    public static void close(@NonNull FragmentManager fm) {
        LoadingDialogFragment dialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (dialog != null)
            dialog.dismiss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Details);

        dialog.setContentView(R.layout.fragment_dialog_loading);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }
}
