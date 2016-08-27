package is.hello.sense.ui.activities;

import android.os.Bundle;

import java.util.List;

import dagger.ObjectGraph;
import is.hello.sense.SenseApplication;
import is.hello.sense.ui.common.InjectionActivity;

public abstract class ScopedInjectionActivity extends InjectionActivity{

    private ObjectGraph scopedObjectGraph;

    public ScopedInjectionActivity(){
        //do nothing here
    }

    public void injectScopedGraph(){
        this.scopedObjectGraph = SenseApplication.getInstance()
                                                 .createScopedObjectGraph(getModules());
        scopedObjectGraph.inject(this);
    }

    abstract List<Object> getModules();

    public void destroyScopedGraph(){
        scopedObjectGraph = null;
    }

    public <T> void injectToScopedGraph(final T object){
        scopedObjectGraph.inject(object);
    }

    @Override
    protected boolean shouldInjectToMainGraphObject() {
        return false;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectScopedGraph();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyScopedGraph();
    }
}
