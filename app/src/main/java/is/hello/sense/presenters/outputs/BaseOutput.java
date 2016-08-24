package is.hello.sense.presenters.outputs;

import android.support.annotation.StringRes;

import is.hello.sense.util.StateSafeExecutor;

public interface BaseOutput extends StateSafeExecutor.Resumes {
    boolean isResumed();

    void showBlockingActivity(@StringRes final int titleRes);

}
