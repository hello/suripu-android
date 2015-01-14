package is.hello.sense.graph.presenters;

import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.sense.api.model.Account;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class AccountPresenterTests extends InjectionTestCase {
    @Inject AccountPresenter accountPresenter;

    public void testUpdate() throws Exception {
        accountPresenter.update();

        SyncObserver<Account> account = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, accountPresenter.account);
        account.await();

        assertNull(account.getError());
        assertNotNull(account.getSingle());
    }

    public void testSaveAccount() throws Exception {
        Account updatedAccount = new Account();
        updatedAccount.setWeight(120L);
        updatedAccount.setHeight(2000L);
        updatedAccount.setBirthDate(LocalDate.now());

        SyncObserver<Account> account = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, accountPresenter.account);
        accountPresenter.saveAccount(updatedAccount);
        account.await();

        assertNull(account.getError());
        assertNotNull(account.getSingle());

        Account savedAccount = account.getLast();
        assertEquals(updatedAccount.getWeight(), savedAccount.getWeight());
        assertEquals(updatedAccount.getHeight(), savedAccount.getHeight());
        assertEquals(updatedAccount.getBirthDate(), savedAccount.getBirthDate());
    }

    public void testUpdateEmail() throws Exception {
        accountPresenter.update();
        SyncObserver<Account> accountBefore = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, accountPresenter.account);
        accountBefore.await();

        assertNull(accountBefore.getError());
        assertNotNull(accountBefore.getLast());


        SyncObserver<Account> accountAfter = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, accountPresenter.updateEmail("test@me.com"));
        accountAfter.await();

        assertNull(accountAfter.getError());
        assertNotNull(accountAfter.getLast());

        assertNotSame(accountBefore.getLast().getEmail(), accountAfter.getLast().getEmail());
        assertEquals("test@me.com", accountAfter.getLast().getEmail());
    }
}
