package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.TransientApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheralTests;
import is.hello.sense.bluetooth.devices.SensePeripheralTests;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.TestBluetoothStack;
import is.hello.sense.bluetooth.stacks.TestBluetoothStackBehavior;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.AccountPresenterTests;
import is.hello.sense.graph.presenters.CurrentConditionsPresenterTests;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.HardwarePresenterTests;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.InsightsPresenterTests;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TimelinePresenterTests;
import is.hello.sense.units.UnitFormatterTests;
import is.hello.sense.util.CachedObjectTests;
import is.hello.sense.util.DateFormatterTests;

@Module(
    library = true,
    injects = {
        TimelinePresenterTests.class,
        TimelinePresenter.class,

        QuestionsPresenterTests.class,
        QuestionsPresenter.class,

        CurrentConditionsPresenterTests.class,
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
        UnitFormatterTests.class,

        HelloPeripheralTests.class,
        SensePeripheralTests.class,
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

    @Provides @GlobalSharedPreferences SharedPreferences provideGlobalSharedPreferences() {
        return applicationContext.getSharedPreferences("test_suite_preferences", Context.MODE_PRIVATE);
    }

    @Singleton @Provides ApiService provideApiService(@NonNull @ApiAppContext Context context, @NonNull ObjectMapper objectMapper) {
        return new TestApiService(context, objectMapper);
    }

    @Singleton @Provides ApiSessionManager provideApiSessionManager() {
        return new TransientApiSessionManager();
    }

    @Singleton @Provides TestBluetoothStackBehavior provideBluetoothStackConfig() {
        return new TestBluetoothStackBehavior();
    }

    @Provides @Singleton BluetoothStack providesBluetoothStack(@NonNull TestBluetoothStackBehavior stackConfig) {
        return new TestBluetoothStack(stackConfig);
    }
}
