package is.hello.sense.flows.home.ui.fragments;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import is.hello.sense.api.model.Account;
import is.hello.sense.flows.home.ui.fragments.AppSettingsFragment;
import is.hello.sense.graph.SenseTestCase;

import static org.robolectric.util.FragmentTestUtil.startFragment;

public class AppSettingsFragmentTest extends SenseTestCase {
    AppSettingsFragment fragment;


    @Before
    public void setUp() throws Exception {
        fragment = new AppSettingsFragment();
        startFragment(fragment);
        fragment = Mockito.spy(fragment);
    }

    @Test
    public void onCreateCallsCorrectMethods() {
        fragment.initializePresenterView();
        fragment.accountInteractor = Mockito.spy(fragment.accountInteractor);
        fragment.onCreate(null);
        Mockito.verify(fragment).addInteractor(Mockito.eq(fragment.accountInteractor));
    }


    @Test
    public void onCreateViewCallsCorrectMethods() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.accountInteractor = Mockito.spy(fragment.accountInteractor);
        fragment.onViewCreated(fragment.getView(), null);
        Mockito.verify(fragment).bindAndSubscribe(Mockito.eq(fragment.accountInteractor.account), Mockito.anyObject(), Mockito.anyObject());
        Mockito.verify(fragment.accountInteractor).update();

    }

    @Test
    public void bindAccount() {
        fragment.presenterView = Mockito.spy(fragment.presenterView);
        fragment.bindAccount(Account.createDefault());
        Mockito.verify(fragment.presenterView).setBreadcrumbVisible(Mockito.eq(false));

    }

}
