package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiModule;
import is.hello.sense.bluetooth.BluetoothModule;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.graph.presenters.InsightsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.SensorHistoryPresenter;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.remote.LastNightWidgetProvider;
import is.hello.sense.remote.RoomConditionsWidgetProvider;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.dialogs.InsightDialogFragment;
import is.hello.sense.ui.dialogs.TimelineEventDialogFragment;
import is.hello.sense.ui.fragments.HomeUndersideFragment;
import is.hello.sense.ui.fragments.QuestionsFragment;
import is.hello.sense.ui.fragments.SensorHistoryFragment;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.fragments.SmartAlarmListFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.Onboarding2ndPillInfoFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingBluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPillFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignIntoWifiFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingWifiNetworkFragment;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.ChangePasswordFragment;
import is.hello.sense.ui.fragments.settings.DeviceDetailsFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.settings.MyInfoFragment;
import is.hello.sense.ui.widget.TimestampTextView;
import is.hello.sense.util.BuildValues;

@Module(
    includes = {ApiModule.class, BluetoothModule.class, DebugModule.class},
    injects = {
        BuildValues.class,
        DebugActivity.class,
        PreferencesPresenter.class,
        TimestampTextView.class,
        NotificationRegistration.class,
        RoomConditionsWidgetProvider.UpdateService.class,
        LastNightWidgetProvider.LastNightService.class,

        LaunchActivity.class,
        HomeActivity.class,

        OnboardingActivity.class,
        OnboardingSignInFragment.class,
        OnboardingRegisterFragment.class,
        OnboardingBluetoothFragment.class,
        OnboardingPairSenseFragment.class,
        HardwarePresenter.class,
        OnboardingWifiNetworkFragment.class,
        OnboardingSignIntoWifiFragment.class,
        OnboardingPairPillFragment.class,
        Onboarding2ndPillInfoFragment.class,

        DeviceListFragment.class,
        DevicesPresenter.class,
        DeviceDetailsFragment.class,

        TimelineFragment.class,
        TimelinePresenter.class,
        TimelineEventDialogFragment.class,

        QuestionsPresenter.class,
        QuestionsFragment.class,

        HomeUndersideFragment.class,
        InsightsPresenter.class,
        InsightDialogFragment.class,
        CurrentConditionsPresenter.class,
        SensorHistoryFragment.class,
        SensorHistoryPresenter.class,

        SmartAlarmListFragment.class,
        SmartAlarmDetailFragment.class,
        SmartAlarmPresenter.class,

        SettingsActivity.RootSettingsFragment.class,
        MyInfoFragment.class,
        AccountSettingsFragment.class,
        ChangePasswordFragment.class,
        AccountPresenter.class,

        UnstableBluetoothFragment.class,
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
