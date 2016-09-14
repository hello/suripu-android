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
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.AccountPresenterTests;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.DeviceIssuesPresenterTests;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.HardwarePresenterTests;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.InsightsPresenterTests;
import is.hello.sense.graph.presenters.PersistentPreferencesPresenter;
import is.hello.sense.graph.presenters.PersistentPreferencesPresenterTests;
import is.hello.sense.graph.presenters.PhoneBatteryPresenter;
import is.hello.sense.graph.presenters.PhoneBatteryPresenterTests;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.RoomConditionsPresenterTests;
import is.hello.sense.graph.presenters.SenseOTAStatusPresenter;
import is.hello.sense.graph.presenters.SenseOTAStatusPresenterTests;
import is.hello.sense.graph.presenters.SenseVoicePresenter;
import is.hello.sense.graph.presenters.SenseVoicePresenterTests;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TimelinePresenterTests;
import is.hello.sense.graph.presenters.UnreadStatePresenterTests;
import is.hello.sense.graph.presenters.UserFeaturesPresenter;
import is.hello.sense.graph.presenters.UserFeaturesPresenterTests;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenter;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenterTests;
import is.hello.sense.graph.presenters.questions.ApiQuestionProviderTests;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProviderTests;
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
            TimelinePresenterTests.class,
            TimelinePresenter.class,

            QuestionsPresenterTests.class,
            QuestionsPresenter.class,
            ApiQuestionProviderTests.class,

            RoomConditionsPresenterTests.class,
            RoomConditionsPresenter.class,

            HardwarePresenter.class,
            HardwarePresenterTests.class,

            InsightsPresenter.class,
            InsightsPresenterTests.class,

            PreferencesPresenter.class,
            PreferencesPresenterTests.class,

            PersistentPreferencesPresenter.class,
            PersistentPreferencesPresenterTests.class,

            UserFeaturesManager.class,
            UserFeaturesPresenter.class,
            UserFeaturesPresenterTests.class,

            AccountPresenter.class,
            AccountPresenterTests.class,

            SmartAlarmPresenter.class,
            SmartAlarmPresenterTests.class,
            SmartAlarmAdapterTests.class,

            DateFormatterTests.class,
            UnitFormatterTests.class,

            ZoomedOutTimelinePresenterTests.class,
            ZoomedOutTimelinePresenter.class,

            DeviceIssuesPresenter.class,
            DeviceIssuesPresenterTests.class,

            LocalUsageTrackerTests.class,
            ReviewQuestionProviderTests.class,
            UnreadStatePresenterTests.class,

            PhoneBatteryPresenter.class,
            PhoneBatteryPresenterTests.class,

            SenseVoicePresenter.class,
            SenseVoicePresenterTests.class,

            SenseOTAStatusPresenter.class,
            SenseOTAStatusPresenterTests.class,
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

    @Singleton @Provides Gson provideGson(@NonNull MarkupProcessor markupProcessor) {
        return ApiModule.createConfiguredGson(markupProcessor);
    }

    @Provides @GlobalSharedPreferences SharedPreferences provideGlobalSharedPreferences() {
        return applicationContext.getSharedPreferences("test_suite_preferences", Context.MODE_PRIVATE);
    }

    @Provides @PersistentSharedPreferences SharedPreferences providePersistentPreferences() {
        return applicationContext.getSharedPreferences("test_suite_persistent_preferences", Context.MODE_PRIVATE);
    }

    @Singleton @Provides ApiService provideApiService(@NonNull @ApiAppContext Context context, @NonNull Gson gson) {
        return new TestApiService(context, gson);
    }

    @Singleton @Provides ApiSessionManager provideApiSessionManager(@NonNull @ApiAppContext Context context) {
        return new TestApiSessionManager(context);
    }

    @Provides BluetoothStack provideBluetoothStack() {
        final BluetoothStack bluetoothStack = mock(BluetoothStack.class);
        doReturn(true)
                .when(bluetoothStack)
                .isEnabled();
        doReturn(Observable.just(true))
                .when(bluetoothStack)
                .enabled();
        return bluetoothStack;
    }

    @Provides BatteryUtil provideBatteryUtil(){
        final BatteryUtil batteryUtil = mock(BatteryUtil.class);
        doReturn(true)
                .when(batteryUtil)
                .isPluggedInAndCharging();
        doReturn(0.5)
                .when(batteryUtil)
                .getBatteryPercentage();
        return batteryUtil;
    }

    @Singleton @Provides UserFeaturesManager provideUserFeaturesManager(
            @NonNull final Context context, @NonNull final Gson gson){
        return new UserFeaturesManager(context, gson);
    }
}
