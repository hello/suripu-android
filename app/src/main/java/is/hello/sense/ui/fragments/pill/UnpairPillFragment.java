package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.presenters.UnpairPillPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;

public class UnpairPillFragment extends BasePresenterFragment implements UnpairPillPresenter.Output {

    private OnboardingSimpleStepView view = null;

    @Inject
    UnpairPillPresenter presenter;

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
                .setSecondaryOnClickListener(this::onSecondaryClick)
                .setToolbarOnHelpClickListener(this::onHelpClick)
                .setDiagramImage(R.drawable.onboarding_sleep_pill);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.onViewCreated();
    }

    @Override
    public void onInjected() {
        addScopedPresenter(presenter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (view != null) {
            view.destroy();
            view = null;
        }
    }

    private void onSecondaryClick(@NonNull final View view) {
        final SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
        dialog.setTitle(R.string.unpair_pill_dialog_title);
        dialog.setMessage(R.string.unpair_pill_dialog_message);
        dialog.setPositiveButton(R.string.action_pair_new_pill, (dialogInterface, i) -> {
            presenter.onPrimaryClick(view);
        });
        dialog.setNegativeButton(R.string.action_dont_pair, (dialogInterface, i) -> {
            finishFlow();
        });
        dialog.show();
    }

    private void onHelpClick(@NonNull final View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIRING_MODE_HELP, null);
        UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.PAIRING_MODE);
    }


    @Override
    public void presentError(@NonNull final Throwable throwable) {
        ErrorDialogFragment.presentError(getActivity(), throwable);
    }

    @Override
    public void postDelayed(@NonNull Runnable runnable, int time) {
        view.postDelayed(runnable, time);
    }

    @Override
    public void finishWithSuccess() {
        finishFlow();
    }

}
