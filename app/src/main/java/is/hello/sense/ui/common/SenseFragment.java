package is.hello.sense.ui.common;

import android.app.Fragment;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;

public class SenseFragment extends Fragment {

    /**
     * Safely pops the fragment from the back stack, propagating a result value
     * and response Intent to the receiver's target fragment.
     * <p/>
     * This method requires a target fragment and the receiver be attached to an activity.
     * @param resultCode    The result code to propagate back.
     * @param response      The result of the fragment.
     * @return  true if the fragment was popped and the target fragment informed; false otherwise.
     */
    protected boolean popFromBackStack(int resultCode, @Nullable Intent response) {
        if (getTargetFragment() != null && getFragmentManager() != null) {
            new Handler().post(() -> getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, response));
            getFragmentManager().popBackStackImmediate();
            return true;
        } else {
            return false;
        }
    }

}
