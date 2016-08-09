package is.hello.sense;


import dagger.Module;
import is.hello.sense.ui.activities.SenseUpdateActivity;
import is.hello.sense.ui.fragments.sense.SenseUpdateIntroFragment;

@Module(complete = false,
        injects = {
                SenseUpdateActivity.class,
                SenseUpdateIntroFragment.class
        }
)
public class SenseUpdateModule {
}
