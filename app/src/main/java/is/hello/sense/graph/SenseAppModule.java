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
import is.hello.sense.graph.presenters.DeviceIssuesPresenter;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.InsightInfoPresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.RoomConditionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.graph.presenters.TrendsPresenter;
import is.hello.sense.graph.presenters.UnreadStatePresenter;
import is.hello.sense.graph.presenters.ZoomedOutTimelinePresenter;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.dialogs.InsightInfoDialogFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.dialogs.SmartAlarmSoundDialogFragment;
import is.hello.sense.ui.fragments.InsightsFragment;
import is.hello.sense.ui.fragments.RoomConditionsFragment;
import is.hello.sense.ui.fragments.SensorHistoryFragment;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.fragments.SmartAlarmListFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineInfoFragment;
import is.hello.sense.ui.fragments.TrendsFragment;
import is.hello.sense.ui.fragments.UndersideFragment;
import is.hello.sense.ui.fragments.ZoomedOutTimelineFragment;
import is.hello.sense.ui.fragments.onboarding.ConnectToWiFiFragment;
import is.hello.sense.ui.fragments.onboarding.Onboarding2ndPillInfoFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterLocationFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterWeightFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.SelectWiFiNetworkFragment;
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
import is.hello.sense.zendesk.ZendeskModule;

@Module(
    includes = {ApiModule.class, BluetoothModule.class, ZendeskModule.class, DebugModule.class},
    injects = {
        SenseApplication.class,
        NotificationReceiver.class,

        DebugActivity.class,
        PreferencesPresenter.class,
        NotificationRegistration.class,
        UnreadStatePresenter.class,

        LaunchActivity.class,
        HomeActivity.class,

        OnboardingActivity.class,
        OnboardingSignInFragment.class,
        OnboardingRegisterFragment.class,
        OnboardingRegisterWeightFragment.class,
        OnboardingRegisterHeightFragment.class,
        OnboardingBluetoothFragment.class,
        OnboardingRegisterAudioFragment.class,
        OnboardingRegisterLocationFragment.class,
        OnboardingUnsupportedDeviceFragment.class,
        OnboardingPairSenseFragment.class,
        HardwarePresenter.class,
        SelectWiFiNetworkFragment.class,
        ConnectToWiFiFragment.class,
        OnboardingPairPillFragment.class,
        Onboarding2ndPillInfoFragment.class,
        OnboardingSenseColorsFragment.class,
        OnboardingRoomCheckFragment.class,

        HardwareFragmentActivity.class,
        DeviceListFragment.class,
        DevicesPresenter.class,
        DeviceIssuesPresenter.class,
        SenseDetailsFragment.class,
        PillDetailsFragment.class,

        TimelineFragment.class,
        TimelinePresenter.class,
        TimelineInfoFragment.class,
        ZoomedOutTimelineFragment.class,
        ZoomedOutTimelinePresenter.class,

        QuestionsPresenter.class,
        QuestionsDialogFragment.class,

        UndersideFragment.class,
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
}
