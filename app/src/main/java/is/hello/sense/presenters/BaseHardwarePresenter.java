package is.hello.sense.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.presenters.outputs.BaseHardwareOutput;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

public abstract class BaseHardwarePresenter<T extends BaseHardwareOutput> extends BaseFragmentPresenter<T> {
    @Inject
    HardwareInteractor hardwareInteractor;

    public BaseHardwarePresenter() {
        super();
        addInteractor(hardwareInteractor);
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
        bindAndSubscribe(hardwareInteractor.runLedAnimation(SenseLedAnimation.STOP),
                         ignored -> onCompletion.run(),
                         e -> {
                             Logger.error(getClass().getSimpleName(), "Error occurred when completing hardware activity", e);

                             onCompletion.run();
                         });
    }


    protected void hideAllActivityForSuccess(@NonNull final Runnable onCompletion,
                                             @NonNull final Action1<Throwable> onError) {
        hideHardwareActivity(() -> viewOutput.hideBlockingActivity(true, onCompletion),
                             e -> viewOutput.hideBlockingActivity(false, () -> onError.call(e)));
    }

    public void hideAllActivityForFailure(@NonNull final Runnable onCompletion) {
        final Runnable next = () -> viewOutput.hideBlockingActivity(false, onCompletion);
        hideHardwareActivity(next, ignored -> next.run());
    }

    protected void hideAllActivityForSuccess(@StringRes final int messageRes,
                                             @NonNull final Runnable onCompletion,
                                             @NonNull final Action1<Throwable> onError) {
        hideHardwareActivity(() -> viewOutput.hideBlockingActivity(messageRes, onCompletion),
                             e -> viewOutput.hideBlockingActivity(false, () -> onError.call(e)));
    }

}
