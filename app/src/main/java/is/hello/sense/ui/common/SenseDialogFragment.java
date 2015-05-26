package is.hello.sense.ui.common;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;

import is.hello.sense.util.StateSafeExecutor;

public class SenseDialogFragment extends DialogFragment implements StateSafeExecutor.Resumes {
    protected final StateSafeExecutor stateSafeExecutor = new StateSafeExecutor(this);


    @Override
    public void onResume() {
        super.onResume();

        stateSafeExecutor.executePendingForResume();
    }

    @Override
    public void onDestroyView() {
        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }


    public int showAllowingStateLoss(@NonNull FragmentManager fm, @NonNull String tag) {
        return fm.beginTransaction()
                .add(this, tag)
                .commitAllowingStateLoss();
    }

    public void dismissSafely() {
        stateSafeExecutor.execute(() -> {
            if (isAdded()) {
                dismiss();
            }
        });
    }
}
