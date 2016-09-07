package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.TestApiSessionManager;
import is.hello.sense.api.sessions.UserFeaturesManager;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.annotations.PersistentSharedPreferences;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.AccountInteractorTests;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.DeviceIssuesInteractorTests;
import is.hello.sense.interactors.InsightsInteractor;
import is.hello.sense.interactors.InsightsInteractorTests;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.interactors.PersistentPreferencesInteractorTests;
import is.hello.sense.interactors.PhoneBatteryInteractor;
import is.hello.sense.interactors.PhoneBatteryInteractorTests;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.PreferencesInteractorTests;
import is.hello.sense.interactors.QuestionsInteractor;
import is.hello.sense.interactors.QuestionsInteractorTests;
import is.hello.sense.interactors.RoomConditionsInteractor;
import is.hello.sense.interactors.RoomConditionsInteractorTests;
import is.hello.sense.interactors.SenseOTAStatusInteractor;
import is.hello.sense.interactors.SenseOTAStatusInteractorTests;
import is.hello.sense.interactors.SenseVoiceInteractor;
import is.hello.sense.interactors.SenseVoiceInteractorTests;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.interactors.SmartAlarmInteractorTests;
import is.hello.sense.interactors.SwapSenseInteractor;
import is.hello.sense.interactors.SwapSenseInteractorTests;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.interactors.TimelineInteractorTests;
import is.hello.sense.interactors.UnreadStateInteractorTests;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.interactors.UserFeaturesInteractorTests;
import is.hello.sense.interactors.ZoomedOutTimelineInteractor;
import is.hello.sense.interactors.ZoomedOutTimelineInteractorTests;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractorTests;
import is.hello.sense.interactors.questions.ApiQuestionProviderTests;
import is.hello.sense.interactors.questions.ReviewQuestionProviderTests;
import is.hello.sense.rating.LocalUsageTrackerTests;
import is.hello.sense.ui.adapter.SmartAlarmAdapterTests;
import is.hello.sense.units.UnitFormatterTests;
import is.hello.sense.util.BatteryUtil;
import is.hello.sense.util.DateFormatterTests;
import is.hello.sense.util.markup.MarkupProcessor;
import rx.Observable;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Module(
    library = true,
    injects = {
            TimelineInteractorTests.class,
            TimelineInteractor.class,

            QuestionsInteractorTests.class,
            QuestionsInteractor.class,
            ApiQuestionProviderTests.class,

            RoomConditionsInteractorTests.class,
            RoomConditionsInteractor.class,

            HardwareInteractor.class,
            HardwareInteractorTests.class,

            InsightsInteractor.class,
            InsightsInteractorTests.class,

            PreferencesInteractor.class,
            PreferencesInteractorTests.class,

            PersistentPreferencesInteractor.class,
            PersistentPreferencesInteractorTests.class,

            UserFeaturesManager.class,
            UserFeaturesInteractor.class,
            UserFeaturesInteractorTests.class,

            AccountInteractor.class,
            AccountInteractorTests.class,

            SmartAlarmInteractor.class,
            SmartAlarmInteractorTests.class,
            SmartAlarmAdapterTests.class,

            DateFormatterTests.class,
            UnitFormatterTests.class,

            ZoomedOutTimelineInteractorTests.class,
            ZoomedOutTimelineInteractor.class,

            DeviceIssuesInteractor.class,
            DeviceIssuesInteractorTests.class,

            LocalUsageTrackerTests.class,
            ReviewQuestionProviderTests.class,
            UnreadStateInteractorTests.class,

            PhoneBatteryInteractor.class,
            PhoneBatteryInteractorTests.class,

            SenseVoiceInteractor.class,
            SenseVoiceInteractorTests.class,

            SenseOTAStatusInteractor.class,
            SenseOTAStatusInteractorTests.class,

            SwapSenseInteractor.class,
            SwapSenseInteractorTests.class
    }
)
@SuppressWarnings("UnusedDeclaration")
public final class TestModule {
    private final Context applicationContext;

    public TestModule(@NonNull Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Provides
    Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides
    @ApiAppContext
    Context provideApiApplicationContext() {
        return applicationContext;
    }

    @Provides
    @Singleton
    MarkupProcessor provideMarkupProcessor() {
        return new MarkupProcessor();
    }

    @Provides
    @Singleton
    Gson provideGson(@NonNull MarkupProcessor markupProcessor) {
        return ApiModule.createConfiguredGson(markupProcessor);
    }

    @Provides
    @GlobalSharedPreferences
    SharedPreferences provideGlobalSharedPreferences() {
        return applicationContext.getSharedPreferences("test_suite_preferences", Context.MODE_PRIVATE);
    }

    @Provides
    @PersistentSharedPreferences
    SharedPreferences providePersistentPreferences() {
        return applicationContext.getSharedPreferences("test_suite_persistent_preferences", Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    ApiService provideApiService(@NonNull @ApiAppContext Context context, @NonNull Gson gson) {
        return new TestApiService(context, gson);
    }

    @Provides
    @Singleton
    ApiSessionManager provideApiSessionManager(@NonNull @ApiAppContext Context context) {
        return new TestApiSessionManager(context);
    }

    @Provides
    BluetoothStack provideBluetoothStack() {
        final BluetoothStack bluetoothStack = mock(BluetoothStack.class);
        doReturn(true)
                .when(bluetoothStack)
                .isEnabled();
        doReturn(Observable.just(true))
                .when(bluetoothStack)
                .enabled();
        return bluetoothStack;
    }

    @Provides
    BatteryUtil provideBatteryUtil(){
        final BatteryUtil batteryUtil = mock(BatteryUtil.class);
        doReturn(true)
                .when(batteryUtil)
                .isPluggedInAndCharging();
        doReturn(0.5)
                .when(batteryUtil)
                .getBatteryPercentage();
        return batteryUtil;
    }

    @Provides
    @Singleton
    UserFeaturesManager provideUserFeaturesManager(
            @NonNull final Context context, @NonNull final Gson gson){
        return new UserFeaturesManager(context, gson);
    }

    @Provides
    @Singleton
    SwapSenseInteractor provideSwapSenseInteractor(final ApiService service){
        return new SwapSenseInteractor(service);
    }
}
