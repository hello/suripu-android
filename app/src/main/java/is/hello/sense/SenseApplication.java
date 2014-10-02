package is.hello.sense;

import android.app.Application;

import com.hello.ble.HelloBle;

import dagger.ObjectGraph;
import is.hello.sense.api.ApiModule;
import is.hello.sense.graph.SenseAppModule;

public class SenseApplication extends Application {
    private static SenseApplication instance = null;
    public static SenseApplication getInstance() {
        return instance;
    }

    private ObjectGraph graph;

    @Override
    public void onCreate() {
        super.onCreate();

        HelloBle.init(this);
        this.graph = ObjectGraph.create(new ApiModule(this), new SenseAppModule(this));

        instance = this;
    }


    public <T> void inject(T target) {
        graph.inject(target);
    }
}
