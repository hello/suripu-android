package is.hello.sense;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.hello.ble.HelloBle;

import net.danlew.android.joda.JodaTimeAndroid;

import dagger.ObjectGraph;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiModule;
import is.hello.sense.graph.SenseAppModule;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Constants;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class SenseApplication extends Application {
    public static final String ACTION_BUILT_GRAPH = SenseApplication.class.getName() + ".ACTION_BUILT_GRAPH";

    private static SenseApplication instance = null;
    public static SenseApplication getInstance() {
        return instance;
    }

    private BuildValues buildValues;
    private ObjectGraph graph;

    @Override
    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);
        HelloBle.init(this);
        CalligraphyConfig.initDefault("fonts/Calibre-Regular.otf", R.attr.fontPath);

        this.buildValues = new BuildValues(this);
        buildGraph();

        instance = this;
    }


    public ApiEnvironment getApiEnvironment() {
        SharedPreferences internalPreferences = getSharedPreferences(Constants.INTERNAL_PREFS, 0);
        String envName = internalPreferences.getString(Constants.INTERNAL_PREF_API_ENV_NAME, buildValues.defaultApiEnvironment);
        return ApiEnvironment.fromString(envName);
    }

    public void buildGraph() {
        this.graph = ObjectGraph.create(new ApiModule(this, getApiEnvironment()), new SenseAppModule(this, buildValues));
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_BUILT_GRAPH));
    }

    public <T> void inject(T target) {
        graph.inject(target);
    }
}
