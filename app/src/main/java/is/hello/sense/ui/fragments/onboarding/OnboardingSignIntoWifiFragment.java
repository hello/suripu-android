package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class OnboardingSignIntoWifiFragment extends Fragment {
    private static final String ARG_SCAN_RESULT = OnboardingSignIntoWifiFragment.class.getName() + ".ARG_SCAN_RESULT";

    public static OnboardingSignIntoWifiFragment newInstance(@Nullable ScanResult network) {
        OnboardingSignIntoWifiFragment fragment = new OnboardingSignIntoWifiFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_SCAN_RESULT, network);
        fragment.setArguments(arguments);

        return fragment;
    }
}
