package is.hello.sense.ui.fragments.onboarding;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;

public class HaveSenseReadyFragment extends SenseFragment {
    //region Lifecycle

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_have_sense_ready)
                .setSubheadingText(R.string.info_have_sense_ready)
                .setDiagramImage(R.drawable.onboarding_sense_grey)
                .setDiagramEdgeToEdge(false)
                .setToolbarWantsBackButton(true)
                .setPrimaryButtonText(R.string.action_pair_your_sense)
                .setPrimaryOnClickListener(this::pairSense)
                .setSecondaryButtonText(R.string.action_buy_sense)
                .setSecondaryOnClickListener(this::showBuySense)
                .withSecondaryOnBottom();
    }

    //endregion


    //region Actions

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    public void pairSense(@NonNull View sender) {
        getOnboardingActivity().pushFragment(new OnboardingRegisterFragment(), null, true);
    }

    public void showBuySense(@NonNull View sender) {
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.ORDER_URL));
    }

    //endregion
}
