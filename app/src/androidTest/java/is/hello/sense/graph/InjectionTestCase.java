package is.hello.sense.graph;

import android.test.InstrumentationTestCase;

import dagger.ObjectGraph;

public class InjectionTestCase extends InstrumentationTestCase {
    private ObjectGraph objectGraph;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (objectGraph == null) {
            this.objectGraph = ObjectGraph.create(new TestModule(getInstrumentation().getTargetContext()));
            objectGraph.inject(this);
        }
    }
}
