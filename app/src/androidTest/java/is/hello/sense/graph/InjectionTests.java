package is.hello.sense.graph;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

import dagger.ObjectGraph;

@RunWith(AndroidJUnit4.class)
public abstract class InjectionTests {
    public InjectionTests() {
        Context context = InstrumentationRegistry.getContext();
        Context targetContext = InstrumentationRegistry.getTargetContext();
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule(context, targetContext));
        objectGraph.inject(this);
    }
}
