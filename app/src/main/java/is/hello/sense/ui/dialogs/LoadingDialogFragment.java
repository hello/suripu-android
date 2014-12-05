package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import is.hello.sense.R;

public class LoadingDialogFragment extends DialogFragment {
    public static final String TAG = LoadingDialogFragment.class.getSimpleName();

    protected static final String ARG_TITLE = LoadingDialogFragment.class.getName() + ".ARG_TITLE";
    protected static final String ARG_WANTS_OPAQUE_BACKGROUND = LoadingDialogFragment.class.getName() + "._ARG_WANTS_OPAQUE_BACKGROUND";

    private TextView titleText;

    public static @NonNull LoadingDialogFragment show(@NonNull FragmentManager fm,
                                                      @Nullable String title,
                                                      boolean wantsOpaqueBackground) {
        Fragment preexistingDialog = fm.findFragmentByTag(TAG);
        if (preexistingDialog != null) {
            fm.beginTransaction()
                    .remove(preexistingDialog)
                    .commit();
        }

        LoadingDialogFragment dialog = LoadingDialogFragment.newInstance(title, wantsOpaqueBackground);
        dialog.show(fm, TAG);

        return dialog;
    }

    public static @NonNull LoadingDialogFragment show(@NonNull FragmentManager fm) {
        return show(fm, null, false);
    }

    public static void close(@NonNull FragmentManager fm) {
        LoadingDialogFragment dialog = (LoadingDialogFragment) fm.findFragmentByTag(TAG);
        if (dialog != null)
            dialog.dismiss();
    }


    public static LoadingDialogFragment newInstance(@Nullable String title, boolean wantsOpaqueBackground) {
        LoadingDialogFragment fragment = new LoadingDialogFragment();

        Bundle arguments = new Bundle();
        if (!TextUtils.isEmpty(title)) {
            arguments.putString(ARG_TITLE, title);
        }
        arguments.putBoolean(ARG_WANTS_OPAQUE_BACKGROUND, wantsOpaqueBackground);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.AppTheme_Dialog_Loading);

        dialog.setContentView(R.layout.fragment_dialog_loading);
        dialog.setCanceledOnTouchOutside(false);

        if (getArguments() != null) {
            Bundle arguments = getArguments();
            if (arguments.getBoolean(ARG_WANTS_OPAQUE_BACKGROUND, false)) {
                View container = dialog.findViewById(R.id.fragment_dialog_loading_container);
                container.setBackgroundColor(getResources().getColor(R.color.background));
            }

            this.titleText = (TextView) dialog.findViewById(R.id.fragment_dialog_loading_title);
            titleText.setText(arguments.getString(ARG_TITLE));
        }

        return dialog;
    }

    public void setTitle(@Nullable String title) {
        getArguments().putString(ARG_TITLE, title);

        if (titleText != null) {
            titleText.setText(title);
        }
    }
}
