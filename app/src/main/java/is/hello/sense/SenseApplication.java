package is.hello.sense;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v4.content.LocalBroadcastManager;

import com.bugsnag.android.Bugsnag;
import com.facebook.FacebookSdk;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.squareup.picasso.LruCache;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;
import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.SenseAppModule;
import is.hello.sense.notifications.NotificationActivityLifecycleListener;
import is.hello.sense.notifications.NotificationMessageReceiver;
import is.hello.sense.notifications.NotificationRegistrationBroadcastReceiver;
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
    @Inject
    NotificationActivityLifecycleListener notificationActivityLifecycleListener;
    @Inject
    NightModeInteractor nightModeInteractor;

    private static SenseApplication instance = null;

    public static SenseApplication getInstance() {
        return instance;
    }

    public static boolean isRunningInRobolectric() {
        return "robolectric".equals(Build.FINGERPRINT);
    }

    public static boolean isLTS() {
        return BuildConfig.FLAVOR.equals("lts");
    }

    public static RefWatcher getRefWatcher() {
        return instance.refWatcher;
    }

    private ObjectGraph graph;

    private RefWatcher refWatcher;

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
            Bugsnag.setReleaseStage(String.format("%s_%s", BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE));
            Bugsnag.setNotifyReleaseStages("full_release", "full_development");
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

        LocalBroadcastManager.getInstance(this)
                             .registerReceiver(
                                     new NotificationRegistrationBroadcastReceiver(),
                                     NotificationRegistrationBroadcastReceiver.getIntentFilter());

        registerActivityLifecycleCallbacks(notificationActivityLifecycleListener);

        if (!isRunningInRobolectric) {
            localUsageTracker.deleteOldUsageStatsAsync();
        }

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        this.refWatcher = LeakCanary.install(this);

        nightModeInteractor.currentNightMode.subscribe(this::onNextNightModeUpdate,
                                                       Functions.LOG_ERROR);

        nightModeInteractor.updateToMatchPrefAndSession(); //if application was destroyed need to update again

        final Observable<Intent> onLogOut = Rx.fromLocalBroadcast(this, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        onLogOut.observeOn(Rx.mainThreadScheduler())
                .subscribe(ignored -> {
                    Logger.info(getClass().getSimpleName(), "Clearing internal preferences.");

                    InternalPrefManager.clearPrefs(this);

                    localUsageTracker.resetAsync();

                    LocalBroadcastManager.getInstance(this)
                                         .sendBroadcast(NotificationRegistrationBroadcastReceiver.getRemoveTokenIntent());

                    NotificationMessageReceiver.cancelShownMessages(this);

                    nightModeInteractor.updateToMatchPrefAndSession();

                    final Intent launchIntent = new Intent(this, LaunchActivity.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(launchIntent);
                }, Functions.LOG_ERROR);
    }

    @Override
    public void onTrimMemory(final int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_MODERATE) {
            clearPicassoCache();
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

    private void clearPicassoCache() {
        Logger.debug(getClass().getSimpleName(), "Clearing picasso memory cache");
        picassoMemoryCache.clear();
    }

    private void onNextNightModeUpdate(@NonNull final Integer mode) {
        clearPicassoCache();
    }
}
