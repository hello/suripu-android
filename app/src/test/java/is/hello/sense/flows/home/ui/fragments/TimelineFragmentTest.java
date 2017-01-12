package is.hello.sense.flows.home.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.LinearLayout;

import org.joda.time.LocalDate;
import org.junit.Test;

import is.hello.sense.FragmentTest;
import is.hello.sense.api.model.v2.Timeline;

import static org.mockito.Mockito.*;

public class TimelineFragmentTest extends FragmentTest<TimelineFragment> {

    @Nullable
    @Override
    protected Class<? extends Activity> activityCreatingFragment() {
        return ActivityWithParent.class;
    }

    @Nullable
    @Override
    protected Bundle startWithArgs() {
        final Bundle args = new Bundle();
        args.putSerializable(TimelineFragment.class.getName() + ".ARG_DATE", LocalDate.now());
        return args;
    }

    @Test
    public void lifeCycleMethodsCallCorrectMethods() {
        fragment.presenterView = null;
        fragment.initializePresenterView();
        verify(fragment).createAdapter();

    }

    private static class ActivityWithParent extends Activity implements TimelineFragment.Parent {

        public ActivityWithParent() {
        }


        @Override
        protected void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final LinearLayout view = new LinearLayout(this, null, 0);
            @IdRes int id = 1;
            view.setId(id);
            setContentView(view);
        }

        @Override
        public int getTutorialContainerIdRes() {
            return 0;
        }

        @Override
        public int getTooltipOverlayContainerIdRes() {
            return 0;
        }

        @Override
        public void jumpToLastNight() {

        }

        @Override
        public void jumpTo(@NonNull LocalDate date, @Nullable Timeline timeline) {

        }
    }

}
