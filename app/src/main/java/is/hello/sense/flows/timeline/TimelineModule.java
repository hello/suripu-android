package is.hello.sense.flows.timeline;

import dagger.Module;
import is.hello.sense.flows.home.ui.fragments.ZoomedOutTimelineFragment;
import is.hello.sense.flows.timeline.ui.activities.TimelineActivity;
import is.hello.sense.ui.fragments.TimelineInfoFragment;

@Module(complete = false, injects = {
        TimelineActivity.class,
        TimelineInfoFragment.class,
        ZoomedOutTimelineFragment.class,
})
public class TimelineModule {


}
