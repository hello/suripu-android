package is.hello.sense;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import net.danlew.android.joda.JodaTimeAndroid;

import javax.inject.Inject;

import dagger.ObjectGraph;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.BluetoothModule;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.SenseAppModule;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import is.hello.sense.util.SessionLogger;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import static rx.android.observables.AndroidObservable.fromLocalBroadcast;

public class SenseApplication extends Application {
    public static final String ACTION_BUILT_GRAPH = SenseApplication.class.getName() + ".ACTION_BUILT_GRAPH";

    private static SenseApplication instance = null;
    public static SenseApplication getInstance() {
        return instance;
    }

    private BuildValues buildValues;
    private ObjectGraph graph;

    @Inject PreferencesPresenter preferences;

    @Override
    public void onCreate() {
        super.onCreate();

        this.buildValues = new BuildValues(this);

        JodaTimeAndroid.init(this);
        CalligraphyConfig.initDefault(Styles.TYPEFACE_ROMAN, R.attr.fontPath);
        Analytics.initialize(this, getString(R.string.build_analytics_api_key));
        if (buildValues.debugScreenEnabled) {
            SessionLogger.init(this);
        }

        buildGraph();

        Observable<Intent> onLogOut = fromLocalBroadcast(this, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT));
        onLogOut.observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignored -> {
                    preferences
                            .edit()
                            .putBoolean(PreferencesPresenter.ONBOARDING_COMPLETED, false)
                            .putInt(PreferencesPresenter.LAST_ONBOARDING_CHECK_POINT, Constants.ONBOARDING_CHECKPOINT_NONE)
                            .apply();
                }, Functions.LOG_ERROR);

        instance = this;
    }


    public ApiEnvironment getApiEnvironment() {
        SharedPreferences internalPreferences = getSharedPreferences(Constants.INTERNAL_PREFS, 0);
        String envName = internalPreferences.getString(Constants.INTERNAL_PREF_API_ENV_NAME, buildValues.defaultApiEnvironment);
        return ApiEnvironment.fromString(envName);
    }

    public void buildGraph() {
        this.graph = ObjectGraph.create(
                new ApiModule(this, getApiEnvironment(), buildValues),
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
