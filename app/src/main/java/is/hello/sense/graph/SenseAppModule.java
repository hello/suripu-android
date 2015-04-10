package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.ApiModule;
import is.hello.sense.bluetooth.BluetoothModule;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.InsightInfoPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.TimelineNavigatorPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.remote.LastNightWidgetProvider;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.dialogs.SmartAlarmSoundDialogFragment;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.fragments.InsightsFragment;
import is.hello.sense.ui.fragments.RoomConditionsFragment;
import is.hello.sense.ui.fragments.SensorHistoryFragment;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.fragments.SmartAlarmListFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineNavigatorFragment;
import is.hello.sense.ui.fragments.TrendsFragment;
import is.hello.sense.ui.fragments.onboarding.Onboarding2ndPillInfoFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment2;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.fragments.settings.ChangeEmailFragment;
import is.hello.sense.ui.fragments.settings.ChangePasswordFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.settings.DeviceTimeZoneFragment;
import is.hello.sense.ui.fragments.settings.NotificationsSettingsFragment;
import is.hello.sense.ui.fragments.settings.PillDetailsFragment;
import is.hello.sense.ui.fragments.settings.SenseDetailsFragment;
import is.hello.sense.ui.fragments.settings.UnitSettingsFragment;

@Module(
    includes = {ApiModule.class, BluetoothModule.class, DebugModule.class},
    injects = {
        SenseApplication.class,
        NotificationReceiver.class,

        DebugActivity.class,
        PreferencesPresenter.class,
        NotificationRegistration.class,
        LastNightWidgetProvider.LastNightService.class,

        LaunchActivity.class,
        HomeActivity.class,

        OnboardingActivity.class,
        OnboardingSignInFragment.class,
        OnboardingRegisterFragment.class,
        OnboardingBluetoothFragment.class,
        OnboardingRegisterAudioFragment.class,
        OnboardingUnsupportedDeviceFragment.class,
        OnboardingPairSenseFragment.class,
        HardwarePresenter.class,
        OnboardingWifiNetworkFragment.class,
        OnboardingSignIntoWifiFragment.class,
        OnboardingPairPillFragment.class,
        Onboarding2ndPillInfoFragment.class,
        OnboardingRoomCheckFragment.class,
        OnboardingRoomCheckFragment2.class,

        DeviceListFragment.class,
        DevicesPresenter.class,
        SenseDetailsFragment.class,
        PillDetailsFragment.class,

        TimelineFragment.class,
        TimelinePresenter.class,
        TimelineEventDialogFragment.class,
        TimelineNavigatorFragment.class,
        TimelineNavigatorPresenter.class,

        QuestionsPresenter.class,
        QuestionsDialogFragment.class,

        InsightsPresenter.class,
        InsightsFragment.class,
        InsightInfoDialogFragment.class,
        InsightInfoPresenter.class,
        RoomConditionsFragment.class,
        RoomConditionsPresenter.class,
        SensorHistoryFragment.class,
        SensorHistoryPresenter.class,
        TrendsPresenter.class,
        TrendsFragment.class,

        SmartAlarmListFragment.class,
        SmartAlarmDetailFragment.class,
        SmartAlarmSoundDialogFragment.class,
        SmartAlarmPresenter.class,

        AppSettingsFragment.class,
        AccountSettingsFragment.class,
        ChangePasswordFragment.class,
        ChangeEmailFragment.class,
        UnitSettingsFragment.class,
        AccountPresenter.class,
        NotificationsSettingsFragment.class,
        DeviceTimeZoneFragment.class,
    }
)
@SuppressWarnings("UnusedDeclaration")
public class SenseAppModule {
    private final Context applicationContext;

    public SenseAppModule(@NonNull Context context) {
        this.applicationContext = context;
    }

    @Provides Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides @GlobalSharedPreferences SharedPreferences provideGlobalSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    /*
    // FOR TESTING ONLY
    @Provides @Singleton QuestionsPresenter provideQuestionPresenter(@NonNull ApiService apiService,
                                                                     @NonNull ApiSessionManager apiSessionManager,
                                                                     @NonNull Context context,
                                                                     @NonNull ObjectMapper objectMapper) {
        return new FakeQuestionsPresenter(apiService, apiSessionManager, context, objectMapper);
    }
    */
}
