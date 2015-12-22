package is.hello.sense;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import com.bugsnag.android.Bugsnag;

import net.danlew.android.joda.JodaTimeAndroid;

import javax.inject.Inject;

import dagger.ObjectGraph;
import is.hello.buruberi.util.Rx;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.BluetoothModule;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.SenseAppModule;
import is.hello.sense.rating.LocalUsageTracker;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SessionLogger;
import is.hello.sense.zendesk.ZendeskModule;
import rx.Observable;

public class SenseApplication extends Application {
    public static final String ACTION_BUILT_GRAPH = SenseApplication.class.getName() + ".ACTION_BUILT_GRAPH";

    @Inject LocalUsageTracker localUsageTracker;

    private static SenseApplication instance = null;
    public static SenseApplication getInstance() {
        return instance;
    }

    public static boolean isRunningInRobolectric() {
        return "robolectric".equals(Build.FINGERPRINT);
    }

    private ObjectGraph graph;

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


        JodaTimeAndroid.init(this);
        if (!isRunningInRobolectric){
            Analytics.initialize(this);
        }
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            SessionLogger.init(this);
        }

        buildGraph();

        if (!isRunningInRobolectric) {
            localUsageTracker.deleteOldUsageStatsAsync();
        }

        Observable<Intent> onLogOut = Rx.fromLocalBroadcast(this, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        onLogOut.observeOn(Rx.mainThreadScheduler())
                .subscribe(ignored -> {
                    Logger.info(getClass().getSimpleName(), "Clearing internal preferences.");

                    getSharedPreferences(Constants.INTERNAL_PREFS, 0)
                            .edit()
                            .clear()
                            .apply();

                    localUsageTracker.resetAsync();
                }, Functions.LOG_ERROR);
    }

    public void buildGraph() {
        this.graph = ObjectGraph.create(
                new ApiModule(this),
                new SenseAppModule(this),
                new BluetoothModule(),
                new ZendeskModule()
        );
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_BUILT_GRAPH));

        graph.inject(this);
    }

    public <T> void inject(T target) {
        graph.inject(target);
    }
}
