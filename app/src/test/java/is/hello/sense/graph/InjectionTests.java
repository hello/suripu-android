package is.hello.sense.graph;

import dagger.ObjectGraph;

public abstract class InjectionTests extends SenseTestCase {
    public InjectionTests() {
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule(getContext().getApplicationContext(), getContext()));
        objectGraph.inject(this);
    }
}
