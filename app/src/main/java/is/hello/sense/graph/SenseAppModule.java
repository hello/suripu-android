package is.hello.sense.graph;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiModule;
import is.hello.sense.graph.presenters.CurrentConditionsPresenter;
import is.hello.sense.graph.presenters.QuestionsPresenter;
import is.hello.sense.graph.presenters.TimelinePresenter;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.SettingsActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.dialogs.TimelineSegmentDetailsDialogFragment;
import is.hello.sense.ui.fragments.HomeUndersideFragment;
import is.hello.sense.ui.fragments.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.OnboardingSignInFragment;
import is.hello.sense.ui.fragments.QuestionsFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.TimelineSegmentView;
import is.hello.sense.util.BuildValues;

@Module(
    includes = {ApiModule.class},
    injects = {
        BuildValues.class,
        DebugActivity.class,

        LaunchActivity.class,
        HomeActivity.class,

        OnboardingActivity.class,
        OnboardingSignInFragment.class,
        OnboardingRegisterFragment.class,

        TimelineFragment.class,
        TimelinePresenter.class,
        TimelineSegmentDetailsDialogFragment.class,
        TimelineSegmentAdapter.class,
        TimelineSegmentView.class,

        QuestionsPresenter.class,
        QuestionsFragment.class,

        HomeUndersideFragment.class,
        CurrentConditionsPresenter.class,
        SettingsActivity.class,
    }
)
@SuppressWarnings("UnusedDeclaration")
public class SenseAppModule {
    private final Context applicationContext;
    private final SharedPreferences sharedPreferences;

    public SenseAppModule(@NonNull Context context) {
        this.applicationContext = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides Context provideApplicationContext() {
        return applicationContext;
    }

    @Provides SharedPreferences provideSharedPreferences() {
        return sharedPreferences;
    }
}
