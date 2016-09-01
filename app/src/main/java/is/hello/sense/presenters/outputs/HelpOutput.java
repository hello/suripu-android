package is.hello.sense.presenters.outputs;

import android.net.Uri;
import android.support.annotation.NonNull;

import is.hello.sense.ui.common.UserSupport;

public interface HelpOutput {
    void showHelpUri(@NonNull final Uri uri);

    void showHelpUri(@NonNull final String uri);

    void showHelpUri(@NonNull final UserSupport.HelpStep helpStep);

    void showHelpUri(@NonNull final UserSupport.DeviceIssue deviceIssue);

}
