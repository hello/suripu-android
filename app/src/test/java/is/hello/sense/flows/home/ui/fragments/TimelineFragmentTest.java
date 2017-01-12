package is.hello.sense.flows.home.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.LocalDate;
import org.junit.Test;

import is.hello.sense.FragmentTest;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.flows.timeline.ui.activities.TimelineActivity;
import rx.Observable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TimelineFragmentTest extends FragmentTest<TimelineFragment> {

    private static final LocalDate date = LocalDate.now();

    @Nullable
    @Override
    protected Class<? extends FragmentTestActivity> activityCreatingFragment() {
        return ActivityWithParent.class;
    }

    @Nullable
    @Override
    protected Bundle startWithArgs() {
        final Bundle args = new Bundle();
        args.putSerializable(TimelineFragment.class.getName() + ".ARG_DATE", date);
        return args;
    }

    @Test
    public void initializePresenterViewTest() {
        fragment.presenterView = null;
        fragment.initializePresenterView();
        verify(fragment).createAdapter();
    }

    @Test
    public void setUserVisibleHintBehavior() {
        fragment.presenterView = null;
        fragment.setUserVisibleHint(true);
        verify(fragment, times(0)).bindIfNeeded();
        fragment.initializePresenterView();
        spyOnPresenterView();
        fragment.setUserVisibleHint(true);
        verify(fragment).bindIfNeeded();
        verify(fragment.presenterView).setAnimationEnabled(eq(true));
        fragment.setUserVisibleHint(false);
        verify(fragment).bindIfNeeded();
        verify(fragment.presenterView).setAnimationEnabled(eq(false));
        verify(fragment).dismissVisibleOverlaysAndDialogs();
        verify(fragment.presenterView).clearHeader();
    }


    @Test
    public void onCreateTest() {
        spyOnTimelineInteractor();
        callOnCreate();
        verify(fragment.timelineInteractor).setDateWithTimeline(eq(date), eq(null));
    }

    @Test
    public void onViewCreatedTest() {
        callOnViewCreated();
        assertNotEquals(0, fragment.toolTipHeight);
        verify(fragment).bindIfNeeded();
    }

    @Test
    public void onPauseTest() {
        spyOnPresenterView();
        callOnPause();
        verify(fragment.presenterView).stopSoundPlayer();
    }

    @Test
    public void onDestroyViewTest() {
        callOnDestroyView();
        verify(fragment).dismissVisibleOverlaysAndDialogs();
    }

    @Test
    public void onDetachTest() {
        assertNotNull(fragment.parent);
        callOnDetach();
        assertNull(fragment.parent);
    }

    @Test
    public void onActivityResultTest() {
        spyOnTimelineInteractor();
        fragment.onActivityResult(80, Activity.RESULT_OK, null);
        verify(fragment.timelineInteractor).update();

        fragment.parent = spy(fragment.parent);
        final Timeline timeline = mock(Timeline.class);
        final Intent data = new Intent();
        data.putExtra(TimelineActivity.class.getSimpleName() + "EXTRA_LOCAL_DATE", date);
        data.putExtra(TimelineActivity.class.getSimpleName() + "EXTRA_TIMELINE", timeline);
        fragment.onActivityResult(101, Activity.RESULT_OK, data);
        verify(fragment.parent).jumpTo(eq(date), eq(timeline));
    }

    @Test
    public void showBreakDownTest() {
        fragment.showBreakDown(mock(Timeline.class));
        verify(fragment).startActivity(any(Intent.class));
    }

    @Test
    public void bindIfNeededTest() {
        spyOnTimelineInteractor();
        fragment.setUserVisibleHint(true);
        assertTrue(fragment.getView() != null);
        assertTrue(fragment.getUserVisibleHint());
        fragment.observableContainer.clearSubscriptions();
        assertFalse(fragment.hasSubscriptions());
        // Multiple calls, should only bind once.
        fragment.bindIfNeeded();
        fragment.bindIfNeeded();
        fragment.bindIfNeeded();
        verify(fragment.timelineInteractor).updateIfEmpty();
        verify(fragment).bindAndSubscribe(eq(fragment.timelineInteractor.timeline), any(), any());
        // preferencesInteractor returns another object that we can't verify. Just make sure bind
        // and subscribe was called twice and assume the second is for preferences.
        verify(fragment, times(2)).bindAndSubscribe(any(), any(), any());
    }

    private void spyOnTimelineInteractor() {
        fragment.timelineInteractor = spy(fragment.timelineInteractor);
    }

    private void spyOnPreferences() {
        fragment.preferences = spy(fragment.preferences);
    }

    public static class ActivityWithParent extends FragmentTestActivity
            implements TimelineFragment.Parent {

        public ActivityWithParent() {

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
        public void jumpTo(@NonNull final LocalDate date,
                           @Nullable final Timeline timeline) {

        }
    }

}
