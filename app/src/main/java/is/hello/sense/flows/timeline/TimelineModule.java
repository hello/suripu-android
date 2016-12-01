package is.hello.sense.flows.timeline;

import dagger.Module;
import is.hello.sense.flows.timeline.ui.activities.TimelineActivity;
import is.hello.sense.ui.fragments.TimelineInfoFragment;
import is.hello.sense.ui.fragments.ZoomedOutTimelineFragment;

@Module(complete = false, injects = {
        TimelineActivity.class,
        TimelineInfoFragment.class,
        ZoomedOutTimelineFragment.class,
})
public class TimelineModule {


}
