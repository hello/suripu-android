package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.R;

public class BuildValues {
    public final String type;
    public final String defaultApiEnvironment;

    public BuildValues(@NonNull Context context) {
        this.type = context.getString(R.string.build_type);
        Crashlytics.setString("BuildValues_type", this.type);

        this.defaultApiEnvironment = context.getString(R.string.build_default_api_env);
    }

    public boolean isDebugBuild() {
        return "debug".equals(this.type);
    }
}
