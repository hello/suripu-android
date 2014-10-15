package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.AccountPresenterTests;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.CurrentConditionsPresenterTests;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TimelinePresenterTests;
import is.hello.sense.units.UnitFormatterTests;

@Module(
    library = true,
    injects = {
        TimelinePresenterTests.class,
        TimelinePresenter.class,

        QuestionsPresenterTests.class,
        QuestionsPresenter.class,

        CurrentConditionsPresenterTests.class,
        CurrentConditionsPresenter.class,

        PreferencesPresenter.class,
        PreferencesPresenterTests.class,

        AccountPresenter.class,
        AccountPresenterTests.class,

        UnitFormatterTests.class,
    }
)
@SuppressWarnings("UnusedDeclaration")
public final class TestModule {
    private final Context applicationContext;
    private final SharedPreferences sharedPreferences;

    public TestModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    @Provides Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides SharedPreferences provideSharedPreferences() {
        return sharedPreferences;
    }

    @Provides @ApiAppContext Context providesApiApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides ObjectMapper provideObjectMapper() {
        return ApiModule.createConfiguredObjectMapper(null);
    }

    @Singleton @Provides ApiService provideApiService(@NonNull @ApiAppContext Context context, @NonNull ObjectMapper objectMapper) {
        return new TestApiService(context, objectMapper);
    }
}
