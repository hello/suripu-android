package is.hello.sense.ui.fragments.pill;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.presenters.UnpairPillPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;

public class UnpairPillFragment extends BasePresenterFragment implements UnpairPillPresenter.Output {

    private OnboardingSimpleStepView view = null;

    @Inject
    UnpairPillPresenter presenter;

    @Override
    public void onInjected() {
        addScopedPresenter(presenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.unpair_pill_title)
                .setSubheadingText(R.string.unpair_pill_message)
                .setPrimaryButtonText(R.string.action_pair_new_sleep_pill)
                .setSecondaryButtonText(R.string.action_do_later)
                .setToolbarWantsHelpButton(true)
                .setPrimaryOnClickListener(presenter::onPrimaryClick)
                .setSecondaryOnClickListener(presenter::onSecondaryClick)
                .setToolbarOnHelpClickListener(presenter::onHelpClick)
                .setDiagramImage(R.drawable.onboarding_sleep_pill);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.onViewCreated();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (view != null) {
            view.destroy();
            view = null;
        }
    }

    @Override
    public void postDelayed(@NonNull final Runnable runnable, final int time) {
        view.postDelayed(runnable, time);
    }

    @Override
    public void finishWithSuccess() {
        finishFlow();
    }

}
