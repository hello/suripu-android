package is.hello.sense.mvp.presenters;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.flows.home.ui.fragments.BacksideFragment;
import is.hello.sense.graph.SenseTestCase;

import static org.robolectric.util.FragmentTestUtil.startFragment;

public class BacksideFragmentTest extends SenseTestCase {
    BacksideFragment fragment;


    @Before
    public void setUp() throws Exception {
        fragment = new BacksideFragment();
        startFragment(fragment);
        fragment = Mockito.spy(fragment);
    }

    @Test
    public void onViewCreatedCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.onViewCreated(fragment.getView(), null);
        Mockito.verify(fragment.presenterView).addOnPageChangeListener(Mockito.eq(fragment));
        Mockito.verify(fragment.presenterView).setOnSelectionChangedListener(Mockito.eq(fragment));
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.unreadStateInteractor.hasUnreadItems), Mockito.anyObject(), Mockito.anyObject());
    }


}