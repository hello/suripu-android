package is.hello.sense.graph.presenters;

import junit.framework.Assert;

import org.joda.time.LocalDate;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.Account;
import is.hello.sense.graph.InjectionTests;
import is.hello.sense.util.Sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class AccountPresenterTests extends InjectionTests {
    @Inject AccountPresenter accountPresenter;

    @Test
    public void update() throws Exception {
        accountPresenter.update();

        Sync.wrap(accountPresenter.account)
            .forEach(Assert::assertNotNull);
    }

    //region Validation

    @Test
    public void normalizeInput() throws Exception {
        assertEquals("", AccountPresenter.normalizeInput(null));
        assertEquals("", AccountPresenter.normalizeInput(""));
        assertEquals("Trailing whitespace", AccountPresenter.normalizeInput("Trailing whitespace  "));
        assertEquals("Leading whitespace", AccountPresenter.normalizeInput("  Leading whitespace"));
        assertEquals("Just a lot of whitespace", AccountPresenter.normalizeInput("  Just a lot of whitespace  "));
    }

    @Test
    public void validateName() throws Exception {
        assertFalse(AccountPresenter.validateName(null));
        assertFalse(AccountPresenter.validateName(""));
        assertTrue(AccountPresenter.validateName("Issac"));
        assertTrue(AccountPresenter.validateName("Issac Newton"));
    }

    @Test
    public void validateEmail() throws Exception {
        assertFalse(AccountPresenter.validateEmail(null));
        assertFalse(AccountPresenter.validateEmail(""));
        assertFalse(AccountPresenter.validateEmail("not a valid email"));
        assertFalse(AccountPresenter.validateEmail("me.com"));
        assertFalse(AccountPresenter.validateEmail("me@me"));
        assertTrue(AccountPresenter.validateEmail("me@me.com"));
        assertTrue(AccountPresenter.validateEmail("me+303@gmail.com"));
        assertTrue(AccountPresenter.validateEmail("my.name@me.com"));
    }

    @Test
    public void validatePassword() throws Exception {
        assertFalse(AccountPresenter.validatePassword(null));
        assertFalse(AccountPresenter.validatePassword(""));
        assertFalse(AccountPresenter.validatePassword("123"));
        assertFalse(AccountPresenter.validatePassword("short"));
        assertTrue(AccountPresenter.validatePassword("longerthan6"));
        assertTrue(AccountPresenter.validatePassword("averylongpasswordindeed"));
    }

    //endregion


    //region Updates

    @Test
    public void saveAccount() throws Exception {
        Account updatedAccount = new Account();
        updatedAccount.setWeight(120);
        updatedAccount.setHeight(2000);
        updatedAccount.setBirthDate(LocalDate.now());

        Account savedAccount = Sync.last(accountPresenter.saveAccount(updatedAccount));
        assertEquals(updatedAccount.getWeight(), savedAccount.getWeight());
        assertEquals(updatedAccount.getHeight(), savedAccount.getHeight());
        assertEquals(updatedAccount.getBirthDate(), savedAccount.getBirthDate());
    }

    @Test
    public void updateEmail() throws Exception {
        Account accountBefore = Sync.wrapAfter(accountPresenter::update, accountPresenter.account).last();
        Account accountAfter = Sync.last(accountPresenter.updateEmail("test@me.com"));
        assertNotSame(accountBefore.getEmail(), accountAfter.getEmail());
        assertEquals("test@me.com", accountAfter.getEmail());
    }

    //endregion
}
