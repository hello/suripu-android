package is.hello.sense.ui.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import is.hello.sense.graph.Scope;
import is.hello.sense.util.Logger;

public abstract class ScopedInjectionActivity extends InjectionActivity implements Scope {
    private @Nullable Map<String, Object> scopedValues;

    @Override
    protected void onPause() {
        super.onPause();

        clearValues();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_RUNNING_MODERATE) {
            clearValues();
        }
    }

    @Override
    public void storeValue(@NonNull String key, @Nullable Object value) {
        if (scopedValues == null) {
            this.scopedValues = new HashMap<>();
        }

        if (value != null) {
            scopedValues.put(key, value);
        } else {
            scopedValues.remove(key);
        }
    }

    @Nullable
    @Override
    public Object retrieveValue(@NonNull String key) {
        if (scopedValues != null) {
            return scopedValues.get(key);
        } else {
            return null;
        }
    }

    public void clearValues() {
        Logger.info(getClass().getSimpleName(), "clearValues()");
        this.scopedValues = null;
    }
}
