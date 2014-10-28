package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.graph.annotations.CacheDirectoryFile;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.AccountPresenterTests;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.CurrentConditionsPresenterTests;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TimelinePresenterTests;
import is.hello.sense.graph.presenters.WifiNetworkPresenter;
import is.hello.sense.graph.presenters.WifiNetworkPresenterTests;
import is.hello.sense.units.UnitFormatterTests;
import is.hello.sense.util.CachedObjectTests;

import static junit.framework.Assert.assertNotNull;

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

        WifiNetworkPresenter.class,
        WifiNetworkPresenterTests.class,

        SmartAlarmPresenter.class,
        SmartAlarmPresenterTests.class,
        CachedObjectTests.class,

        UnitFormatterTests.class,
    }
)
@SuppressWarnings("UnusedDeclaration")
public final class TestModule {
    private final Context applicationContext;
    private final Context targetContext;

    public TestModule(@NonNull Context applicationContext, @NonNull Context targetContext) {
        this.applicationContext = applicationContext;
        this.targetContext = targetContext;
    }

    @Provides Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides @ApiAppContext Context providesApiApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides ObjectMapper provideObjectMapper() {
        return ApiModule.createConfiguredObjectMapper(null);
    }

    @Provides @CacheDirectoryFile File provideCacheDirectoryFile() {
        File cacheFile = targetContext.getExternalCacheDir();
        if (cacheFile == null) {
            cacheFile = targetContext.getCacheDir();
        }

        assertNotNull(cacheFile);

        return cacheFile;
    }

    @Provides @GlobalSharedPreferences SharedPreferences provideGlobalSharedPreferences() {
        return applicationContext.getSharedPreferences("test_suite_preferences", Context.MODE_PRIVATE);
    }

    @Singleton @Provides ApiService provideApiService(@NonNull @ApiAppContext Context context, @NonNull ObjectMapper objectMapper) {
        return new TestApiService(context, objectMapper);
    }

    @Provides SmartAlarmPresenter provideSmartAlarmPresenter(@NonNull ApiService apiService,
                                                             @CacheDirectoryFile @Nullable File cacheDirectory,
                                                             @NonNull ObjectMapper objectMapper) {
        return new SmartAlarmPresenterTests.StubedSmartAlarmPresenter(apiService, cacheDirectory, objectMapper);
    }
}
