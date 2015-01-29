package is.hello.sense.graph.presenters;

import junit.framework.Assert;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.sense.api.model.Account;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

public class AccountPresenterTests extends InjectionTestCase {
    @Inject AccountPresenter accountPresenter;

    public void testUpdate() throws Exception {
        accountPresenter.update();

        Sync.wrap(accountPresenter.account)
            .forEach(Assert::assertNotNull);
    }

    public void testSaveAccount() throws Exception {
        Account updatedAccount = new Account();
        updatedAccount.setWeight(120);
        updatedAccount.setHeight(2000);
        updatedAccount.setBirthDate(LocalDate.now());

        Account savedAccount = Sync.last(accountPresenter.saveAccount(updatedAccount));
        assertEquals(updatedAccount.getWeight(), savedAccount.getWeight());
        assertEquals(updatedAccount.getHeight(), savedAccount.getHeight());
        assertEquals(updatedAccount.getBirthDate(), savedAccount.getBirthDate());
    }

    public void testUpdateEmail() throws Exception {
        Account accountBefore = Sync.wrapAfter(accountPresenter::update, accountPresenter.account).last();
        Account accountAfter = Sync.last(accountPresenter.updateEmail("test@me.com"));
        assertNotSame(accountBefore.getEmail(), accountAfter.getEmail());
        assertEquals("test@me.com", accountAfter.getEmail());
    }
}
