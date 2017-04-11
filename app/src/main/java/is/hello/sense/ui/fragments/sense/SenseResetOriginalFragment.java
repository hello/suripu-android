package is.hello.sense.ui.fragments.sense;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.SenseResetOriginalPresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepView;


public class SenseResetOriginalFragment extends BasePresenterFragment
        implements SenseResetOriginalPresenter.Output {

    @Inject
    SenseResetOriginalPresenter presenter;

    private OnboardingSimpleStepView view;

    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_sense_reset_original)
                .setSubheadingText(R.string.info_sense_reset_original)
                .setToolbarOnHelpClickListener(this::onHelp)
                .setDiagramImage(R.drawable.sense_upgrade_original)
                .setWantsSecondaryButton(false)
                .setPrimaryButtonText(R.string.action_reset_sense_original)
                .setPrimaryOnClickListener(this::onNext);
        return view;
    }

    public void onHelp(final View ignored) {
        presenter.navigateToHelp();
    }

    public void onNext(final View ignored) {
        presenter.onStart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(this.view != null) {
            this.view.destroy();
            this.view = null;
        }
    }

    //region PresenterOutput implementation

    @Override
    public void showRetry(@StringRes final int retryRes,
                          @NonNull final Runnable onClickRunnable) {
        this.view.setSecondaryButtonText(retryRes)
                 .setSecondaryOnClickListener(ignore -> onClickRunnable.run());
    }

    //endregion
}
