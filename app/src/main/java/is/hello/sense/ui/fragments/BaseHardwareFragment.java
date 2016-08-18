package is.hello.sense.ui.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.UserFeaturesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

/**
 * Extends InjectionFragment to add support for displaying
 * in-app and on Sense loading indicators.
 */
public abstract class BaseHardwareFragment extends InjectionFragment {
    public
    @Inject
    HardwarePresenter hardwarePresenter;
    @Inject
    protected UserFeaturesPresenter userFeaturesPresenter;

    private LoadingDialogFragment loadingDialogFragment;


    protected boolean isPairOnlySession() {
        return getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_PAIR_ONLY, false);
    }

    protected boolean shouldReleasePeripheralOnPair() {
        return getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_RELEASE_PERIPHERAL_ON_PAIR, true);
    }


    //region Activity

    protected void showBlockingActivity(@StringRes final int titleRes) {
        if (loadingDialogFragment == null) {
            stateSafeExecutor.execute(() -> this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                                                    getString(titleRes),
                                                                                                    LoadingDialogFragment.OPAQUE_BACKGROUND));
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    protected void hideBlockingActivity(@StringRes final int text, @Nullable final Runnable onCompletion) {
        stateSafeExecutor
                .execute(() -> LoadingDialogFragment
                        .closeWithMessageTransition(getFragmentManager(),
                                                    () -> {
                                                        this.loadingDialogFragment = null;
                                                        if (onCompletion != null) {
                                                            stateSafeExecutor.execute(onCompletion);
                                                        }
                                                    },
                                                    text));
    }

    protected void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
        stateSafeExecutor.execute(() -> {
            if (success) {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    stateSafeExecutor.execute(onCompletion);
                });
            } else {
                LoadingDialogFragment.close(getFragmentManager());
                this.loadingDialogFragment = null;
                onCompletion.run();
            }
        });
    }


    protected void showHardwareActivity(@NonNull final Runnable onCompletion,
                                        @NonNull final Action1<Throwable> onError) {
        bindAndSubscribe(hardwarePresenter.runLedAnimation(SenseLedAnimation.BUSY),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when showing hardware activity.", e);
                             onError.call(e);
                         });
    }

    protected void hideHardwareActivity(@NonNull final Runnable onCompletion,
                                        @Nullable final Action1<Throwable> onError) {
        if (hardwarePresenter.isConnected()) {
            bindAndSubscribe(hardwarePresenter.runLedAnimation(SenseLedAnimation.TRIPPY),
                             ignored -> onCompletion.run(),
                             e -> {
                                 Logger.error(getClass().getSimpleName(), "Error occurred when hiding hardware activity.", e);
                                 if (onError != null) {
                                     onError.call(e);
                                 } else {
                                     onCompletion.run();
                                 }
                             });
        } else {
            stateSafeExecutor.execute(onCompletion);
        }
    }

    protected void completeHardwareActivity(@NonNull final Runnable onCompletion) {
        bindAndSubscribe(hardwarePresenter.runLedAnimation(SenseLedAnimation.STOP),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when completing hardware activity", e);

                             onCompletion.run();
                         });
    }


    protected void hideAllActivityForSuccess(@NonNull final Runnable onCompletion,
                                             @NonNull final Action1<Throwable> onError) {
        hideHardwareActivity(() -> hideBlockingActivity(true, onCompletion),
                             e -> hideBlockingActivity(false, () -> onError.call(e)));
    }

    protected void hideAllActivityForFailure(@NonNull final Runnable onCompletion) {
        final Runnable next = () -> hideBlockingActivity(false, onCompletion);
        hideHardwareActivity(next, ignored -> next.run());
    }

    //endregion



}
