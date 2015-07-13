package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.test.TestBluetoothStack;
import is.hello.buruberi.bluetooth.stacks.test.TestBluetoothStackBehavior;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.TransientApiSessionManager;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.AccountPresenterTests;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.HardwarePresenterTests;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.InsightsPresenterTests;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.RoomConditionsPresenterTests;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TimelinePresenterTests;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.graph.presenters.TrendsPresenterTests;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenter;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenterTests;
import is.hello.sense.ui.adapter.DevicesAdapterTests;
import is.hello.sense.util.CachedObjectTests;
import is.hello.sense.util.DateFormatterTests;
import is.hello.sense.util.markup.MarkupProcessor;

@Module(
    library = true,
    injects = {
        TimelinePresenterTests.class,
        TimelinePresenter.class,

        QuestionsPresenterTests.class,
        QuestionsPresenter.class,

        RoomConditionsPresenterTests.class,
        RoomConditionsPresenter.class,

        HardwarePresenter.class,
        HardwarePresenterTests.class,

        InsightsPresenter.class,
        InsightsPresenterTests.class,

        PreferencesPresenter.class,
        PreferencesPresenterTests.class,

        AccountPresenter.class,
        AccountPresenterTests.class,

        SmartAlarmPresenter.class,
        SmartAlarmPresenterTests.class,
        CachedObjectTests.class,

        DateFormatterTests.class,

        ZoomedOutTimelinePresenterTests.class,
        ZoomedOutTimelinePresenter.class,

        TrendsPresenterTests.class,
        TrendsPresenter.class,

        DevicesAdapterTests.class,
    }
)
@SuppressWarnings("UnusedDeclaration")
public final class TestModule {
    private final Context applicationContext;

    public TestModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Provides Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides @ApiAppContext Context provideApiApplicationContext() {
        return applicationContext;
    }

    @Singleton @Provides MarkupProcessor provideMarkupProcessor() {
        return new MarkupProcessor();
    }

    @Singleton @Provides ObjectMapper provideObjectMapper(@NonNull MarkupProcessor markupProcessor) {
        return ApiModule.createConfiguredObjectMapper(markupProcessor);
    }

    @Provides @GlobalSharedPreferences SharedPreferences provideGlobalSharedPreferences() {
        return applicationContext.getSharedPreferences("test_suite_preferences", Context.MODE_PRIVATE);
    }

    @Singleton @Provides ApiService provideApiService(@NonNull @ApiAppContext Context context, @NonNull ObjectMapper objectMapper) {
        return new TestApiService(context, objectMapper);
    }

    @Singleton @Provides ApiSessionManager provideApiSessionManager(@NonNull @ApiAppContext Context context) {
        return new TransientApiSessionManager(context);
    }

    @Singleton @Provides TestBluetoothStackBehavior provideBluetoothStackConfig() {
        return new TestBluetoothStackBehavior();
    }

    @Provides @Singleton BluetoothStack providesBluetoothStack(@NonNull TestBluetoothStackBehavior stackConfig) {
        return new TestBluetoothStack(stackConfig);
    }
}
