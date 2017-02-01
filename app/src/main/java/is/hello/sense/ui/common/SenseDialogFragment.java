package is.hello.sense.ui.common;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.NonNull;

import is.hello.sense.util.Constants;
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

    /**
     * @return default int of {@link FragmentTransaction#commitAllowingStateLoss()}
     * or {@link is.hello.sense.util.Constants#NONE} if {@link FragmentManager#isDestroyed()}
     * to prevent starting new transaction as activity is being destroyed
     */
    public int showAllowingStateLoss(@NonNull final FragmentManager fm,
                                     @NonNull final String tag) {
        if(fm.isDestroyed()) {
            return Constants.NONE;
        }
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
