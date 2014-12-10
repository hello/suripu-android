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

public class OnboardingPillPartnerInfoFragment extends SenseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_partner_info, container, false);

        Button continueButton = (Button) view.findViewById(R.id.fragment_onboarding_partner_info_continue);
        continueButton.setOnClickListener(ignored -> finishWithResult(Activity.RESULT_OK, null));

        return view;
    }
}
