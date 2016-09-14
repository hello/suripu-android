package is.hello.sense.interactors;

import junit.framework.Assert;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.io.File;

import javax.inject.Inject;

import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.v2.MultiDensityImage;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Sync;
import retrofit.mime.TypedFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AccountInteractorTests extends InjectionTestCase {
    @Inject
    AccountInteractor accountPresenter;

    @Test
    public void update() throws Exception {
        accountPresenter.update();

        Sync.wrap(accountPresenter.account)
            .forEachAction(Assert::assertNotNull);
    }

    //region Validation

    @Test
    public void normalizeInput() throws Exception {
        assertEquals("", AccountInteractor.normalizeInput(null));
        assertEquals("", AccountInteractor.normalizeInput(""));
        assertEquals("Trailing whitespace", AccountInteractor.normalizeInput("Trailing whitespace  "));
        assertEquals("Leading whitespace", AccountInteractor.normalizeInput("  Leading whitespace"));
        assertEquals("Just a lot of whitespace", AccountInteractor.normalizeInput("  Just a lot of whitespace  "));
    }

    @Test
    public void validateName() throws Exception {
        assertFalse(AccountInteractor.validateName(null));
        assertFalse(AccountInteractor.validateName(""));
        assertTrue(AccountInteractor.validateName("Issac"));
        assertTrue(AccountInteractor.validateName("Issac Newton"));
    }

    @Test
    public void validateEmail() throws Exception {
        assertFalse(AccountInteractor.validateEmail(null));
        assertFalse(AccountInteractor.validateEmail(""));
        assertFalse(AccountInteractor.validateEmail("not a valid email"));
        assertFalse(AccountInteractor.validateEmail("me.com"));
        assertFalse(AccountInteractor.validateEmail("me@me"));
        assertTrue(AccountInteractor.validateEmail("me@me.com"));
        assertTrue(AccountInteractor.validateEmail("me+303@gmail.com"));
        assertTrue(AccountInteractor.validateEmail("my.name@me.com"));
    }

    @Test
    public void validatePassword() throws Exception {
        assertFalse(AccountInteractor.validatePassword(null));
        assertFalse(AccountInteractor.validatePassword(""));
        assertFalse(AccountInteractor.validatePassword("123"));
        assertFalse(AccountInteractor.validatePassword("short"));
        assertTrue(AccountInteractor.validatePassword("longerthan6"));
        assertTrue(AccountInteractor.validatePassword("averylongpasswordindeed"));
    }

    //endregion


    //region Updates

    @Test
    public void saveAccount() throws Exception {
        final Account updatedAccount = new Account();
        updatedAccount.setWeight(120);
        updatedAccount.setHeight(2000);
        updatedAccount.setBirthDate(LocalDate.now());

        final Account savedAccount = Sync.last(accountPresenter.saveAccount(updatedAccount));
        assertEquals(updatedAccount.getWeight(), savedAccount.getWeight());
        assertEquals(updatedAccount.getHeight(), savedAccount.getHeight());
        assertEquals(updatedAccount.getBirthDate(), savedAccount.getBirthDate());
    }

    @Test
    public void updateEmail() throws Exception {
        final Account accountBefore = Sync.wrapAfter(accountPresenter::update, accountPresenter.account).last();
        final Account accountAfter = Sync.last(accountPresenter.updateEmail("test@me.com"));
        assertNotSame(accountBefore.getEmail(), accountAfter.getEmail());
        assertEquals("test@me.com", accountAfter.getEmail());
    }

    @Test
    public void updateProfilePhoto() throws Exception {
        final File testFile = new File("src/tests/assets/photos/test_profile_photo.jpg");
        final TypedFile typedFile = new TypedFile("multipart/form-data", testFile);
        accountPresenter.setWithPhoto(false);
        final Account accountBefore = Sync.wrapAfter(accountPresenter::update, accountPresenter.account).last();
        final MultiDensityImage imageUploaded = Sync.last(accountPresenter
                                                         .updateProfilePicture(typedFile,
                                                                               Analytics.Account.EVENT_CHANGE_PROFILE_PHOTO,
                                                                               Analytics.ProfilePhoto.Source.CAMERA));
        accountPresenter.setWithPhoto(true);
        final Account accountAfter =  Sync.last(accountPresenter.provideUpdateObservable());

        assertNotSame(accountAfter.getProfilePhoto(), accountBefore.getProfilePhoto());
        assertEquals(imageUploaded, accountAfter.getProfilePhoto());
    }

    @Test
    public void deleteProfilePhoto() throws Exception {
        accountPresenter.setWithPhoto(true);
        final Account accountBefore = Sync.wrapAfter(accountPresenter::update, accountPresenter.account).last();
        accountPresenter.deleteProfilePicture().doOnNext(ignore -> {
            accountPresenter.setWithPhoto(false);
            final Account accountAfter = Sync.last(accountPresenter.provideUpdateObservable());
            assertNull(accountAfter.getProfilePhoto());
        });

        assertNotNull(accountBefore.getProfilePhoto());
    }

    //endregion
}
