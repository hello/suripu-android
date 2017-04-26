package is.hello.sense.flows.home.ui.fragments;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import is.hello.buruberi.util.Rx;
import is.hello.sense.FragmentTest;
import is.hello.sense.api.model.v2.alerts.Category;
import is.hello.sense.flows.home.util.OnboardingFlowProvider;
import is.hello.sense.graph.Scope;
import is.hello.sense.ui.activities.OnboardingActivity;
import rx.Scheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class HomePresenterFragmentTest extends FragmentTest<HomePresenterFragment> {

    private static final int TIMELINE_FRAGMENT_POSITION = 0;
    private static final int STARTING_FRAGMENT_POSITION = 0;

    @NonNull
    @Override
    protected Class<? extends FragmentTestActivity> activityCreatingFragment() {
        return TestHomeActivity.class;
    }

    @Test
    public void initializePresenterView()  {
        fragment.presenterView = null;
        callInitializePresenterView();
        verify(fragment).createAdapter();
    }

    @Test
    public void onViewCreated()  {
        spyOnPresenterView();
        callOnViewCreated();
        verify(fragment.presenterView).setTabListener(fragment);
        verify(fragment.presenterView).setCurrentItem(STARTING_FRAGMENT_POSITION);
        verify(fragment).addInteractor(fragment.deviceIssuesInteractor);
        verify(fragment).addInteractor(fragment.alertsInteractor);
        verify(fragment).addInteractor(fragment.lastNightInteractor);
        verify(fragment).addInteractor(fragment.unreadStateInteractor);
        verify(fragment).bindAndSubscribe(eq(fragment.alertsInteractor.alert), any(), any());
        verify(fragment).bindAndSubscribe(eq(fragment.lastNightInteractor.timeline), any(), any());
        verify(fragment).bindAndSubscribe(eq(fragment.unreadStateInteractor.hasUnreadItems), any(), any());
        verify(fragment).checkInForUpdates();
    }

    @Test
    public void tabChangedOnTimelineFragmentPosition() throws Exception {
        spyOnPresenterView();
        fragment.tabChanged(TIMELINE_FRAGMENT_POSITION);
        verify(fragment.presenterView).setCurrentItem(TIMELINE_FRAGMENT_POSITION);
        verify(fragment).jumpToLastNight();
    }

    @Test
    public void selectTabMethods() throws Exception {
        spyOnPresenterView();
        fragment.selectTimelineTab();
        verify(fragment.presenterView).setCurrentItem(TIMELINE_FRAGMENT_POSITION);
        fragment.selectTrendsTab();
        verify(fragment.presenterView).setCurrentItem(1);
        fragment.selectFeedTab();
        verify(fragment.presenterView).setCurrentItem(2);
        fragment.selectSoundTab();
        verify(fragment.presenterView).setCurrentItem(3);
        fragment.selectConditionsTab();
        verify(fragment.presenterView).setCurrentItem(4);

    }

    @Test
    public void handleAlert() throws Exception {
        doAnswer(invocation -> null).when(fragment).unMuteSense();
        doAnswer(invocation -> null).when(fragment).startDevicesActivity();
        fragment.handleAlert(Category.SENSE_MUTED);
        verify(fragment, times(1)).unMuteSense();
        fragment.handleAlert(Category.SENSE_NOT_PAIRED);
        fragment.handleAlert(Category.SENSE_NOT_PAIRED);
        fragment.handleAlert(Category.SLEEP_PILL_NOT_PAIRED);
        fragment.handleAlert(Category.SLEEP_PILL_NOT_SEEN);
        verify(fragment, times(4)).startDevicesActivity();
    }

    public static class TestHomeActivity extends FragmentTestActivity
    implements Scope, OnboardingFlowProvider {

        public TestHomeActivity() {
            super();
        }

        @NonNull
        @Override
        public Scheduler getScopeScheduler() {
            return Rx.mainThreadScheduler();
        }

        @Override
        public void storeValue(@NonNull String key, @Nullable Object value) {

        }

        @Nullable
        @Override
        public Object retrieveValue(@NonNull String key) {
            return null;
        }

        @Override
        public int getOnboardingFlow() {
            return OnboardingActivity.FLOW_NONE;
        }
    }

}