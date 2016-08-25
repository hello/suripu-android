package is.hello.sense.presenters.outputs;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.util.StateSafeExecutor;

public interface BaseOutput extends StateSafeExecutor.Resumes {
    boolean isResumed();

    void showBlockingActivity(@StringRes final int titleRes);

    void hideBlockingActivity(final boolean success, @NonNull final Runnable onComplete);

    void hideBlockingActivity(@StringRes final int messageRes, @NonNull final Runnable onComplete);
}
