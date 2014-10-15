package is.hello.sense.graph;

import android.content.Context;
import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiModule;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.dialogs.TimelineSegmentDetailsDialogFragment;
import is.hello.sense.ui.fragments.HomeUndersideFragment;
import is.hello.sense.ui.fragments.QuestionsFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.settings.MyInfoFragment;
import is.hello.sense.ui.widget.TimelineSegmentView;
import is.hello.sense.ui.widget.TimestampTextView;
import is.hello.sense.util.BuildValues;

@Module(
    includes = {ApiModule.class},
    injects = {
        BuildValues.class,
        DebugActivity.class,
        PreferencesPresenter.class,
        TimestampTextView.class,

        LaunchActivity.class,
        HomeActivity.class,

        OnboardingActivity.class,
        OnboardingSignInFragment.class,
        OnboardingRegisterFragment.class,

        TimelineFragment.class,
        TimelinePresenter.class,
        TimelineSegmentDetailsDialogFragment.class,
        TimelineSegmentView.class,

        QuestionsPresenter.class,
        QuestionsFragment.class,

        HomeUndersideFragment.class,
        CurrentConditionsPresenter.class,
        SettingsActivity.class,
        MyInfoFragment.class,
        AccountPresenter.class,
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
}
