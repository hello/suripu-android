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
    public void onDestroyViewTest() {
        callInitializePresenterView();
        spyOnPresenterView();
        final ViewPagerPresenterView presenterView = fragment.presenterView; // destroy sets this to null. Hold a reference
        callOnDestroyView();
        Mockito.verify(presenterView).removeViewPagerListener(Mockito.eq(fragment));
    }

    @Test
    public void onPageScrolledTest() {
        fragment.onPageScrolled(10, 1000, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, 100, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, 50, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, 25, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, 10, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, 1, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, .9f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(0.79999995f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, .5f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(0.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, .4f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(0.19999999f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, .1f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(0.8f));
        Mockito.reset(fragment);
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, -.5f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, -5f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, -50f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
        fragment.onPageScrolled(10, -500f, 5);
        Mockito.verify(fragment).setFabSize(Mockito.eq(1.0f));
        Mockito.reset(fragment);
    }
}
