package is.hello.sense.ui.fragments.sense;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.presenters.SenseResetOriginalPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;


public class SenseResetOriginalFragment extends InjectionFragment
        implements SenseResetOriginalPresenter.SenseResetOriginalPresenterOutput {

    @Inject
    SenseResetOriginalPresenter presenter;

    private OnboardingSimpleStepView view;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_sense_reset_original)
                .setSubheadingText(R.string.info_sense_reset_original_intro)
                .setToolbarOnHelpClickListener(this::onHelp)
                .setDiagramImage(R.drawable.onboarding_sense_intro)
                .setSecondaryButtonText(R.string.action_do_later)
                .setSecondaryOnClickListener(this::onDone)
                .setPrimaryButtonText(R.string.action_reset_sense)
                .setPrimaryOnClickListener(this::onNext);
        presenter.setView(this);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(presenter.getInteractorSubject(),
                         presenter::onInteractorOutputNext,
                         presenter::onInteractorOutputError);
    }

    public void onHelp(final View ignored) {
        presenter.navigateToHelp(getActivity());
    }

    public void onDone(final View ignored) {
        //todo this would also be handled by the presenter directing to a router
        finishFlow();
    }

    public void onNext(final View ignored) {
        presenter.startNetworkCall();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        view.destroy();
        view = null;
        presenter.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Any other call to this method is due to configuration change or low memory.
        // We want to release the presenter only when the fragment is truly done.
        presenter.onDestroy();
        presenter = null;
        stateSafeExecutor.clearPending();
    }

    //region PresenterOutput implementation

    @Override
    public void showProgress() {
        LoadingDialogFragment.show(getFragmentManager());
    }

    @Override
    public void hideProgress() {
        LoadingDialogFragment.close(getFragmentManager());
    }

    @Override
    public void onNetworkCallSuccess() {
        view.setSubheadingText(R.string.info_sense_reset_original_done)
            .setDiagramImage(R.drawable.onboarding_pair_sense)
            .setPrimaryOnClickListener(this::onDone)
            .setPrimaryButtonText(R.string.action_done)
            .setWantsSecondaryButton(false);
    }

    @Override
    public void onNetworkCallFailure(@NonNull final Throwable e) {
        ErrorDialogFragment.presentError(getActivity(), e);
    }

    //endregion
}
