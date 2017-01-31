package is.hello.sense.flows.home.ui.fragments;

import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.FragmentTest;
import is.hello.sense.flows.home.util.SoundsViewPagerPresenterDelegate;
import is.hello.sense.mvp.view.ViewPagerPresenterView;

import static junit.framework.Assert.assertTrue;

public class SoundsPresenterFragmentTest extends FragmentTest<SoundsPresenterFragment> {


    @Test
    public void newViewPagerDelegateInstanceTest() {
        assertTrue(fragment.newViewPagerDelegateInstance() instanceof SoundsViewPagerPresenterDelegate);
    }

    @Test
    public void onViewCreatedTest() {
        spyOnPresenterView();
        callOnViewCreated();
        Mockito.verify(fragment.presenterView).addViewPagerListener(Mockito.eq(fragment));
    }

    @Test
    public void shouldAddViewPagerListenerTest() {
        assertTrue(fragment.shouldAddViewPagerListener());
    }


}
