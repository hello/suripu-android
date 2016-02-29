package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.commonsense.service.SenseServiceConnection;
import is.hello.sense.api.ApiAppContext;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.TestApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.TestApiSessionManager;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.AccountPresenterTests;
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.DeviceIssuesPresenterTests;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.InsightsPresenterTests;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenterTests;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenterTests;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.RoomConditionsPresenterTests;
import is.hello.sense.graph.presenters.SensePresenter;
import is.hello.sense.graph.presenters.SensePresenterTests;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenterTests;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TimelinePresenterTests;
import is.hello.sense.graph.presenters.UnreadStatePresenterTests;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenter;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenterTests;
import is.hello.sense.graph.presenters.questions.ApiQuestionProviderTests;
import is.hello.sense.graph.presenters.questions.ReviewQuestionProviderTests;
import is.hello.sense.rating.LocalUsageTrackerTests;
import is.hello.sense.ui.adapter.SmartAlarmAdapterTests;
import is.hello.sense.units.UnitFormatterTests;
import is.hello.sense.util.DateFormatterTests;
import is.hello.sense.util.markup.MarkupProcessor;
import rx.Observable;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Module(library = true,
        injects = {
                TimelinePresenterTests.class,
                TimelinePresenter.class,

                QuestionsPresenterTests.class,
                QuestionsPresenter.class,
                ApiQuestionProviderTests.class,

                RoomConditionsPresenterTests.class,
                RoomConditionsPresenter.class,

                InsightsPresenter.class,
                InsightsPresenterTests.class,

                PreferencesPresenter.class,
                PreferencesPresenterTests.class,

                AccountPresenter.class,
                AccountPresenterTests.class,

                SmartAlarmPresenter.class,
                SmartAlarmPresenterTests.class,
                SmartAlarmAdapterTests.class,

                SensePresenter.class,
                SensePresenterTests.class,

                DateFormatterTests.class,
                UnitFormatterTests.class,

                ZoomedOutTimelinePresenterTests.class,
                ZoomedOutTimelinePresenter.class,

                DeviceIssuesPresenter.class,
                DeviceIssuesPresenterTests.class,

                LocalUsageTrackerTests.class,
                ReviewQuestionProviderTests.class,
                UnreadStatePresenterTests.class,
        })
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

    @Provides SenseServiceConnection provideSenseServiceConnection() {
        return mock(SenseServiceConnection.class);
    }
}
