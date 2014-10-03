package is.hello.sense.graph;

import android.content.Context;
import android.support.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.ApiModule;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.activities.LaunchActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.TimelineSegmentAdapter;
import is.hello.sense.ui.fragments.TemporaryOnboardingFragment;
import is.hello.sense.ui.fragments.TimelineFragment;
import is.hello.sense.ui.widget.SegmentView;

@Module(
    includes = {ApiModule.class},
    injects = {
        LaunchActivity.class,
        HomeActivity.class,
        OnboardingActivity.class,

        TemporaryOnboardingFragment.class,
        TimelineFragment.class,

        TimelineSegmentAdapter.class,

        SegmentView.class,
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
