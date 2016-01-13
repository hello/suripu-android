package is.hello.sense.graph;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Scheduler;

public interface Scope {
    @NonNull Scheduler getScopeScheduler();
    void storeValue(@NonNull String key, @Nullable Object value);
    @Nullable Object retrieveValue(@NonNull String key);
}
