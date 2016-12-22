package is.hello.sense.interactors;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import is.hello.sense.api.model.VoiceResponse;
import is.hello.sense.graph.InjectionTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class SenseVoiceInteractorTests extends InjectionTestCase{
    @Inject
    SenseVoiceInteractor senseVoiceInteractor;
    @Inject
    AccountInteractor accountPresenter;
    @Inject
    PersistentPreferencesInteractor persistentPreferencesPresenter;

    private static final DateTime DEFAULT_DATE_TIME = DateTime.now();

    @After
    public void resetPresenter(){
        senseVoiceInteractor.reset();
        //accountPresenter.logOut();
        persistentPreferencesPresenter.clear();
    }

    @Test
    public void withValidResponse() {
        assertTrue(senseVoiceInteractor.isValidResponse(new VoiceResponse(DEFAULT_DATE_TIME.plusDays(1), null, null, null, VoiceResponse.Result.OK)));
    }

    @Test
    public void withInValidResponse() {
        assertFalse(senseVoiceInteractor.isValidResponse(new VoiceResponse(DEFAULT_DATE_TIME.minusDays(1), null, null, null, VoiceResponse.Result.OK)));
    }

    @Test
    public void updateValidResponseTime() {
        assertTrue(DEFAULT_DATE_TIME.minusDays(1).isBefore(senseVoiceInteractor.getLastValidResponseTime()));
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.OK));
        assertEquals(senseVoiceInteractor.getLastValidResponseTime(), DEFAULT_DATE_TIME.getMillis());
    }

    @Test
    public void with3FailCount() {
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.NONE));
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.NONE));
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.NONE));
        assertEquals(3, senseVoiceInteractor.getFailCount());
    }

    @Test
    public void with0FailCount() {
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.OK));
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.OK));
        senseVoiceInteractor.updateState(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.OK));
        assertEquals(0, senseVoiceInteractor.getFailCount());
    }

    @Test
    public void getMostRecent() {
        final VoiceResponse newResponse = new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.OK);
        final VoiceResponse oldResponse = new VoiceResponse(DEFAULT_DATE_TIME.minusDays(1), null, null, null, VoiceResponse.Result.OK);

        final VoiceResponse returnedResponse = SenseVoiceInteractor.getMostRecent(
                new ArrayList<>(Arrays.asList(oldResponse, newResponse)));

        assertEquals(returnedResponse, newResponse);
    }

    @Test
    public void getNullResponse(){
        final VoiceResponse returnedResponse = SenseVoiceInteractor.getMostRecent(new ArrayList<>(0));

        assertNull(returnedResponse);
    }

    @Test
    public void hasSuccessfulTrue(){
        assertTrue(SenseVoiceInteractor.hasSuccessful(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.OK)));
    }

    @Test
    public void hasSuccessfulFalse(){
        assertFalse(SenseVoiceInteractor.hasSuccessful(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.REJECTED)));
        assertFalse(SenseVoiceInteractor.hasSuccessful(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.UNKNOWN)));
        assertFalse(SenseVoiceInteractor.hasSuccessful(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.TRY_AGAIN)));
        assertFalse(SenseVoiceInteractor.hasSuccessful(new VoiceResponse(DEFAULT_DATE_TIME, null, null, null, VoiceResponse.Result.NONE)));
    }

    @Test
    public void updateHasCompletedTutorialTrue() {
        /*final String FAKE_ID = "fake_id";
        final Account fakeAccount = new Account();
        fakeAccount.setId(FAKE_ID);
        Sync.last(accountPresenter.saveAccount(fakeAccount));

        senseVoiceInteractor.updateHasCompletedTutorial(true);
        final boolean hasCompletedTutorial = persistentPreferencesPresenter.hasCompletedVoiceTutorial(FAKE_ID);
        assertTrue(hasCompletedTutorial);*/
    }
}
