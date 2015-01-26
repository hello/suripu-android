package is.hello.sense;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import net.danlew.android.joda.JodaTimeAndroid;

import dagger.ObjectGraph;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.BluetoothModule;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.SenseAppModule;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Logger;
import is.hello.sense.util.SessionLogger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class SenseApplication extends Application {
    public static final String ACTION_BUILT_GRAPH = SenseApplication.class.getName() + ".ACTION_BUILT_GRAPH";

    private static SenseApplication instance = null;
    public static SenseApplication getInstance() {
        return instance;
    }

    private ObjectGraph graph;

    @Override
    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);
        Analytics.initialize(this, getString(R.string.build_analytics_api_key));
        SessionLogger.init(this);

        buildGraph();

        Observable<Intent> onLogOut = fromLocalBroadcast(this, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        onLogOut.observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignored -> {
                    Logger.info(getClass().getSimpleName(), "Clearing internal preferences.");

                    getSharedPreferences(Constants.INTERNAL_PREFS, 0)
                            .edit()
                            .clear()
                            .apply();
                }, Functions.LOG_ERROR);

        instance = this;
    }


    public ApiEnvironment getApiEnvironment() {
        SharedPreferences internalPreferences = getSharedPreferences(Constants.INTERNAL_PREFS, 0);
        String envName = internalPreferences.getString(Constants.INTERNAL_PREF_API_ENV_NAME, getString(R.string.build_default_api_env));
        return ApiEnvironment.fromString(envName);
    }

    public void buildGraph() {
        this.graph = ObjectGraph.create(
                new ApiModule(this, getApiEnvironment()),
                new SenseAppModule(this),
                new BluetoothModule()
        );
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_BUILT_GRAPH));

        graph.inject(this);
    }

    public <T> void inject(T target) {
        graph.inject(target);
    }
}
