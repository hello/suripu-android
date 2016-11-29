package is.hello.sense;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v4.content.LocalBroadcastManager;

import com.bugsnag.android.Bugsnag;
import com.facebook.FacebookSdk;
import com.squareup.picasso.LruCache;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;
import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.SenseAppModule;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.InternalPrefManager;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SessionLogger;
import rx.Observable;

public class SenseApplication extends MultiDexApplication {
    public static final String ACTION_BUILT_GRAPH = SenseApplication.class.getName() + ".ACTION_BUILT_GRAPH";

    @Inject
    LocalUsageTracker localUsageTracker;
    @Inject
    LruCache picassoMemoryCache;

    private static SenseApplication instance = null;

    public static SenseApplication getInstance() {
        return instance;
    }

    public static boolean isRunningInRobolectric() {
        return "robolectric".equals(Build.FINGERPRINT);
    }

    private ObjectGraph graph;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Always do this first.
        SenseApplication.instance = this;

        // And always do this second.
        final boolean isRunningInRobolectric = isRunningInRobolectric();
        if (!isRunningInRobolectric) {
            Bugsnag.init(this);
            Bugsnag.setReleaseStage(BuildConfig.BUILD_TYPE);
            Bugsnag.setNotifyReleaseStages("release");
        }

        FacebookSdk.sdkInitialize(getApplicationContext());

        JodaTimeAndroid.init(this);
        if (!isRunningInRobolectric) {
            Analytics.initialize(this);
        }
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            SessionLogger.init(this);
        }

        buildGraph();

        if (!isRunningInRobolectric) {
            localUsageTracker.deleteOldUsageStatsAsync();
        }

        final Observable<Intent> onLogOut = Rx.fromLocalBroadcast(this, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        onLogOut.observeOn(Rx.mainThreadScheduler())
                .subscribe(ignored -> {
                    Logger.info(getClass().getSimpleName(), "Clearing internal preferences.");

                    InternalPrefManager.clearPrefs(this);

                    localUsageTracker.resetAsync();

                    final Intent launchIntent = new Intent(this, LaunchActivity.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(launchIntent);
                }, Functions.LOG_ERROR);
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_MODERATE) {
            Logger.debug(getClass().getSimpleName(), "Clearing picasso memory cache");
            picassoMemoryCache.clear();
        }
    }

    public void buildGraph() {
        this.graph = ObjectGraph.create(
                new ApiModule(this),
                new SenseAppModule(this)
                                       );
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_BUILT_GRAPH));

        graph.inject(this);
    }

    public <T> void inject(final T target) {
        graph.inject(target);
    }

    /**
     * @param modules add additional modules that should be removed in the dependent Activity's onDestroy.
     */
    public ObjectGraph createScopedObjectGraph(final List<Object> modules) {
        return graph.plus(modules.toArray());
    }
}
