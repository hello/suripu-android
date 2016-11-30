package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.SenseApplication;
import is.hello.sense.SenseOTAModule;
import is.hello.sense.api.ApiModule;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.fb.FacebookApiModule;
import is.hello.sense.bluetooth.BluetoothModule;
import is.hello.sense.debug.TestActivity;
import is.hello.sense.flows.home.interactors.AlertsInteractor;
import is.hello.sense.flows.home.ui.activities.HomeActivity;
import is.hello.sense.flows.home.ui.activities.NewHomeActivity;
import is.hello.sense.flows.home.ui.fragments.AppSettingsFragment;
import is.hello.sense.flows.home.ui.fragments.BacksideFragment;
import is.hello.sense.flows.home.ui.fragments.HomeFragment;
import is.hello.sense.flows.home.ui.fragments.InsightsFragment;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsFragment;
import is.hello.sense.flows.home.ui.fragments.SoundsFragment;
import is.hello.sense.flows.home.ui.fragments.TrendsFragment;
import is.hello.sense.flows.home.ui.fragments.VoiceFragment;
import is.hello.sense.graph.annotations.GlobalSharedPreferences;
import is.hello.sense.graph.annotations.PersistentSharedPreferences;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.InsightInfoInteractor;
import is.hello.sense.interactors.InsightsInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.QuestionsInteractor;
import is.hello.sense.interactors.RoomConditionsInteractor;
import is.hello.sense.interactors.SleepDurationsInteractor;
import is.hello.sense.interactors.SleepSoundsInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.interactors.TimelineInteractor;
import is.hello.sense.interactors.TrendsInteractor;
import is.hello.sense.interactors.UnreadStateInteractor;
import is.hello.sense.interactors.ZoomedOutTimelineInteractor;
import is.hello.sense.interactors.hardware.HardwareInteractor;
import is.hello.sense.mvp.presenters.SoundsPresenterFragment;
import is.hello.sense.notifications.NotificationReceiver;
import is.hello.sense.notifications.NotificationRegistration;
import is.hello.sense.pill.PillModule;
import is.hello.sense.presenters.BaseHardwarePresenter;
import is.hello.sense.settings.SettingsModule;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.dialogs.InsightInfoFragment;
import is.hello.sense.ui.dialogs.QuestionsDialogFragment;
import is.hello.sense.ui.dialogs.SmartAlarmSoundDialogFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.TimelineInfoFragment;
import is.hello.sense.ui.fragments.ZoomedOutTimelineFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingPairPill;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterFragment;
import is.hello.sense.ui.fragments.onboarding.SignInFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.settings.PillDetailsFragment;
import is.hello.sense.ui.fragments.settings.SenseDetailsFragment;
import is.hello.sense.ui.fragments.sounds.SleepSoundsFragment;
import is.hello.sense.ui.fragments.sounds.SmartAlarmListFragment;
import is.hello.sense.ui.widget.SleepSoundsPlayerView;
import is.hello.sense.util.Constants;
import is.hello.sense.util.UtilityModule;
import is.hello.sense.zendesk.ZendeskModule;

@Module(
        includes = {
                ApiModule.class,
                BluetoothModule.class,
                ZendeskModule.class,
                DebugModule.class,
                SettingsModule.class,
                UtilityModule.class,
                FacebookApiModule.class,
                PillModule.class,
                SenseOTAModule.class, //todo remove after converting fragments to use presenters
        },
        injects = {
                SenseApplication.class,
                NotificationReceiver.class,

                DebugActivity.class,
                PreferencesInteractor.class,
                NotificationRegistration.class,
                UnreadStateInteractor.class,

                LaunchActivity.class,
                HomeActivity.class,
                NewHomeActivity.class,

                SignInFragment.class,
                RegisterFragment.class,
                HardwareInteractor.class,

                HardwareFragmentActivity.class,
                DeviceListFragment.class,
                DevicesInteractor.class,
                DeviceIssuesInteractor.class,
                SenseDetailsFragment.class,
                PillDetailsFragment.class,

                TimelineFragment.class,
                TimelineInteractor.class,
                TimelineInfoFragment.class,
                ZoomedOutTimelineFragment.class,
                ZoomedOutTimelineInteractor.class,

                QuestionsInteractor.class,
                QuestionsDialogFragment.class,

                InsightsInteractor.class,
                InsightInfoInteractor.class,
                InsightInfoFragment.class,
                RoomConditionsInteractor.class,

                TrendsInteractor.class,
                SmartAlarmListFragment.class,
                SleepSoundsFragment.class,
                SmartAlarmSoundDialogFragment.class,
                SmartAlarmInteractor.class,
                SleepSoundsInteractor.class,
                SleepDurationsInteractor.class,
                ListActivity.class,
                SleepSoundsPlayerView.class,
                BaseHardwarePresenter.class,

                //todo remove when fragments use presenters
                OnboardingSenseColorsFragment.class,
                OnboardingPairPill.class,
                BacksideFragment.class,
                RoomConditionsFragment.class,
                TrendsFragment.class,
                InsightsFragment.class,
                SoundsFragment.class,
                HomeFragment.class,
                VoiceFragment.class,
                AppSettingsFragment.class,
                TestActivity.class,
                SoundsPresenterFragment.class, // todo erase when done testing


        }
)
@SuppressWarnings("UnusedDeclaration")
public class SenseAppModule {
    private final Context applicationContext;

    public SenseAppModule(@NonNull final Context context) {
        this.applicationContext = context;
    }

    @Provides
    Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides
    @GlobalSharedPreferences
    SharedPreferences provideGlobalSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    @Provides
    @PersistentSharedPreferences
    SharedPreferences providePersistentSharedPreferences(@NonNull final Context context) {
        return context.getSharedPreferences(Constants.PERSISTENT_PREFS, Context.MODE_PRIVATE);
    }


    //todo move to individual module settings/onboarding/upgrading/home
    @Provides
    @Singleton
    DevicesInteractor providesDevicesInteractor(@NonNull final ApiService apiService) {
        return new DevicesInteractor(apiService);
    }

    @Provides
    @Singleton
    public AlertsInteractor providesAlertsInteractor(@NonNull final ApiService apiService){
        return new AlertsInteractor(apiService);
    }

}
