package is.hello.sense.ui.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class SenseFragment extends Fragment {

    /**
     * Shows the fragment using a fragment manager within a given container.
     *
     * @param fm          The fragment manager to show the fragment from.
     * @param containerId The container to show the fragment within.
     * @param tag         The tag to associate with the fragment.
     */
    public void show(@NonNull final FragmentManager fm,
                     @IdRes final int containerId,
                     @NonNull final String tag) {
        fm.beginTransaction()
          .add(containerId, this, tag)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
          .addToBackStack(tag)
          .commitAllowingStateLoss();
    }

    /**
     * How this method behaves depends on whether or not there is a target fragment set:
     * <p>
     * <ol>
     * <li>If the fragment has a target fragment and result code, the target fragment
     * will receive the result of the fragment invocation, and the receiver will
     * be popped from the back stack.</li>
     * <li>If the fragment does not have a target fragment, its containing activity
     * will have its result code and intent and be told to finish.</li>
     * </ol>
     *
     * @param resultCode The result code to propagate back.
     * @param response   The result of the fragment.
     * @return true if the fragment was popped and the target fragment informed; false otherwise.
     */
    protected boolean finishWithResult(final int resultCode, @Nullable final Intent response) {
        if (getFragmentManager() != null) {
            if (getTargetFragment() != null) {
                new Handler().post(() -> getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, response));
                getFragmentManager().popBackStackImmediate();
            } else {
                getActivity().setResult(RESULT_OK, response);
                getActivity().finish();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Resolve the navigation container of the fragment.
     * <p>
     * This method first checks if the target fragment
     * conforms to {@link FragmentNavigation}, then checks
     * if the activity conforms to {@link FragmentNavigation}.
     * If neither does, this method returns <code>null</code>.
     *
     * @return The navigation container of the fragment if it has one; null otherwise.
     */
    public FragmentNavigation getFragmentNavigation() {
        if (getTargetFragment() instanceof FragmentNavigation) {
            return (FragmentNavigation) getTargetFragment();
        } else if (getActivity() instanceof FragmentNavigation) {
            return (FragmentNavigation) getActivity();
        } else {
            return null;
        }
    }

    public void finishFlow() {
        finishFlowWithResult(RESULT_OK);
    }

    public void cancelFlow() {
        finishFlowWithResult(RESULT_CANCELED);
    }

    public void finishFlowWithResult(final int resultCode) {
        finishFlowWithResult(resultCode, null);
    }

    public void finishFlowWithResult(final int resultCode, @Nullable final Intent intent) {
        final FragmentNavigation fragmentNavigation = getFragmentNavigation();
        if (fragmentNavigation != null) {
            fragmentNavigation.flowFinished(this, resultCode, intent);
        }
    }
}
