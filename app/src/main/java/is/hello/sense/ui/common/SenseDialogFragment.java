package is.hello.sense.ui.common;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.NonNull;
import android.view.View;

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


    /**
     * Showing without allowing state-loss has been a source of many crashes.
     * Use SenseDialogFragment's {@link #showAllowingStateLoss(FragmentManager, String)}
     * method instead to reduce chances of user crashes.
     */
    @Deprecated
    @Override
    public int show(@NonNull FragmentTransaction transaction, String tag) {
        return super.show(transaction, tag);
    }

    /**
     * Showing without allowing state-loss has been a source of many crashes.
     * Use SenseDialogFragment's {@link #showAllowingStateLoss(FragmentManager, String)}
     * method instead to reduce chances of user crashes.
     */
    @Deprecated
    @Override
    public void show(@NonNull FragmentManager manager, String tag) {
        super.show(manager, tag);
    }

    public int showAllowingStateLoss(@NonNull FragmentManager fm, @NonNull String tag) {
        return fm.beginTransaction()
                 .add(this, tag)
                 .commitAllowingStateLoss();
    }
    public int showAllowingStateLossWithTransition(@NonNull FragmentManager fm, @NonNull String tag, @NonNull View view, @NonNull String transitionName) {
        return fm.beginTransaction()
                 .add(this, tag)
                 .addSharedElement(view, transitionName)
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
