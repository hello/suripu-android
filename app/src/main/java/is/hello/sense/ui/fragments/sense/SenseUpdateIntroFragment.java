package is.hello.sense.ui.fragments.sense;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.presenters.SenseUpdateIntroPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class SenseUpdateIntroFragment extends BasePresenterFragment
        implements SenseUpdateIntroPresenter.Output {

    @Inject
    SenseUpdateIntroPresenter presenter;

    private OnboardingSimpleStepView view;


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_upgrade_sense_voice)
                .setSubheadingText(R.string.info_upgrade_sense_voice)
                .setDiagramImage(R.drawable.onboarding_sense_intro)
                .setPrimaryButtonText(R.string.action_set_up)
                .setPrimaryOnClickListener(presenter::onPrimaryClicked)
                .setSecondaryButtonText(R.string.action_upgrade_sense_voice_help)
                .setSecondaryOnClickListener(presenter::onSecondaryClicked)
                .setToolbarWantsBackButton(true);

        return view;
    }

    @Override
    public void onInjected() {
        addScopedPresenter(presenter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view.destroy();
        view = null;
    }

    @Override
    public void showHelpUrl() {
        //todo replace with proper uri
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.ORDER_URL));
    }
}
