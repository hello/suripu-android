package is.hello.sense.ui.common;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Provides lifecycle methods to call for saving state to {@link android.os.Bundle}
 */
public interface StateSaveable {

    boolean isStateRestored();

    void onRestoreState(@NonNull final Bundle savedState);

    @Nullable
    Bundle onSaveState();

    @NonNull
    String getSavedStateKey();
}
