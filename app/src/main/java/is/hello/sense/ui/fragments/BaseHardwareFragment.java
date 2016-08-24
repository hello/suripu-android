package is.hello.sense.ui.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Logger;
import rx.functions.Action1;
/**
 * Extends InjectionFragment to add support for displaying
 * in-app and on Sense loading indicators.
 */
public abstract class BaseHardwareFragment extends ScopedInjectionFragment {

    private LoadingDialogFragment loadingDialogFragment;

    protected boolean isPairOnlySession() {
        return getActivity().getIntent().getBooleanExtra(OnboardingActivity.EXTRA_PAIR_ONLY, false);
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
        bindAndSubscribe(hardwareInteractor.runLedAnimation(SenseLedAnimation.BUSY),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when showing hardware activity.", e);
                             onError.call(e);
                         });
    }

    protected void hideHardwareActivity(@NonNull final Runnable onCompletion,
                                        @Nullable final Action1<Throwable> onError) {
        if (hardwareInteractor.isConnected()) {
            bindAndSubscribe(hardwareInteractor.runLedAnimation(SenseLedAnimation.TRIPPY),
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
        hardwareInteractor.runLedAnimation(SenseLedAnimation.STOP),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when completing hardware activity", e);

                             onCompletion.run();
                         };
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

    protected void hideAllActivityForSuccess(@StringRes final int messageRes,
                                             @NonNull final Runnable onCompletion,
                                             @NonNull final Action1<Throwable> onError) {
        hideHardwareActivity(() -> hideBlockingActivity(messageRes, onCompletion),
                             e -> hideBlockingActivity(false, () -> onError.call(e)));
    }
    //endregion



}
