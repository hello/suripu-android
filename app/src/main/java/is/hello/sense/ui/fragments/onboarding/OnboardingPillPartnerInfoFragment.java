package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingPillPartnerInfoFragment extends SenseFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_ONBOARDING_PAIRING_MODE_OFF, null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_partner_info, container, false);

        Button continueButton = (Button) view.findViewById(R.id.fragment_onboarding_partner_info_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> finishWithResult(Activity.RESULT_OK, null));

        return view;
    }
}
