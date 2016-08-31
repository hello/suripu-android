package is.hello.sense.presenters.outputs;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.StateSafeExecutor;

public interface BaseOutput extends StateSafeExecutor.Resumes {
    boolean isResumed();

    boolean canObservableEmit();

    void showBlockingActivity(@StringRes final int titleRes);

    void hideBlockingActivity(final boolean success, @NonNull final Runnable onComplete);

    void hideBlockingActivity(@StringRes final int messageRes, @NonNull final Runnable onComplete);

    void showErrorDialog(@NonNull final ErrorDialogFragment.PresenterBuilder builder);

    void showHelpUri(@NonNull final Uri uri);

    void showHelpUri(@NonNull final String  uri);

    void showHelpUri(@NonNull final UserSupport.HelpStep helpStep);

    void finishActivity();

    void finishFlowWithResult(int resultCode);
}
