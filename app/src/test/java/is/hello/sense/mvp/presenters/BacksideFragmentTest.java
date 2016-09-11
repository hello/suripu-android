package is.hello.sense.mvp.presenters;


import static org.robolectric.util.FragmentTestUtil.startFragment;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.mvp.presenters.home.BacksideFragment;

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
        fragment.presenterView = Mockito.spy(fragment.getPresenterView());
        fragment.accountInteractor = Mockito.spy(fragment.accountInteractor);
        fragment.onViewCreated(fragment.getView(), null);
        Mockito.verify(fragment.presenterView).setAdapter(fragment.getFragmentManager());
        Mockito.verify(fragment.presenterView).addOnPageChangeListener(fragment);
        Mockito.verify(fragment.presenterView).setOnSelectionChangedListener(fragment);
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.unreadStateInteractor.hasUnreadItems), Mockito.anyObject(), Mockito.anyObject());
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.accountInteractor.account), Mockito.anyObject(), Mockito.anyObject());
        Mockito.verify(fragment.accountInteractor).update();
    }


}