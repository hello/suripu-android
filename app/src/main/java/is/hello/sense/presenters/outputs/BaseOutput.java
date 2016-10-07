package is.hello.sense.presenters.outputs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.StateSafeExecutor;

public interface BaseOutput extends
        StateSafeExecutor.Resumes,
        HelpOutput,
        FlowOutput {

    boolean isResumed();

    boolean canObservableEmit();

    void showBlockingActivity(@StringRes final int titleRes);

    void hideBlockingActivity(final boolean success, @NonNull final Runnable onComplete);

    void hideBlockingActivity(@StringRes final int messageRes, @Nullable final Runnable onComplete);

    void showErrorDialog(@NonNull final ErrorDialogFragment.PresenterBuilder builder);

    void showAlertDialog(@NonNull final SenseAlertDialog.Builder builder);

}
