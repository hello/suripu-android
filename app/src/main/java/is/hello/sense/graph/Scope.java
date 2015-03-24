package is.hello.sense.graph;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface Scope {
    void storeValue(@NonNull String key, @Nullable Object value);
    @Nullable Object retrieveValue(@NonNull String key);
}
