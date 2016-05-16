package is.hello.sense.graph;

import dagger.ObjectGraph;

public abstract class InjectionTestCase extends SenseTestCase {
    public InjectionTestCase() {
        final ObjectGraph objectGraph =
                ObjectGraph.create(new TestModule(getContext().getApplicationContext()));
        objectGraph.inject(this);
    }
}
