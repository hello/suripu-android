package is.hello.sense.ui.activities.appcompat;

import android.os.Bundle;

import java.util.Collections;
import java.util.List;

import dagger.ObjectGraph;
import is.hello.sense.SenseApplication;
public abstract class ScopedInjectionAppCompatActivity extends InjectionActivity {

    private ObjectGraph scopedObjectGraph;

    public ScopedInjectionAppCompatActivity() {
        //do nothing here
    }

    public void injectScopedGraph() {
        this.scopedObjectGraph = SenseApplication.getInstance()
                                                 .createScopedObjectGraph(getModules());
        scopedObjectGraph.inject(this);
    }

    protected List<Object> getModules() {
        return Collections.emptyList();
    }

    public void destroyScopedGraph() {
        scopedObjectGraph = null;
    }

    public <T> void injectToScopedGraph(final T object) {
        scopedObjectGraph.inject(object);
    }

    @Override
    protected boolean shouldInjectToMainGraphObject() {
        return false;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        injectScopedGraph();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyScopedGraph();
    }
}
