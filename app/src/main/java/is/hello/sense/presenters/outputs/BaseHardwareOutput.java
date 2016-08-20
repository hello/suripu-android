package is.hello.sense.presenters.outputs;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

public interface BaseHardwareOutput extends BaseOutput{
    void hideBlockingActivity(@StringRes final int messageRes, @NonNull final Runnable onCompletion);
    void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion);

}
