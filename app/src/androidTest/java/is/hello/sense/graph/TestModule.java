package is.hello.sense.graph;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.graph.presenters.CurrentConditionsPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenterTests;

@Module(
    library = true,
    injects = {
        TimelinePresenterTests.class,
        QuestionsPresenterTests.class,
        CurrentConditionsPresenterTests.class
    }
)
public final class TestModule {
    private final Context applicationContext;

    public TestModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Provides Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides @ApiAppContext Context providesApiApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides ApiService provideApiService() {
        return new TestApiService();
    }
}
