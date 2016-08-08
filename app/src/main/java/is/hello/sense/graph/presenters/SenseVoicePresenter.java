package is.hello.sense.graph.presenters;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoiceResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.schedulers.Schedulers;

public class SenseVoicePresenter extends ValuePresenter<VoiceResponse> {

    @Inject
    ApiService apiService;
    @Inject
    PersistentPreferencesPresenter preferences;
    @Inject
    AccountPresenter accountPresenter;

    @Inject SenseVoicePresenter(){}


    public final static long POLL_INTERVAL = 8; //todo test with real 1.5 senses to adjust
    public final PresenterSubject<VoiceResponse> voiceResponse = this.subject;
    private int failCount = 0;

    @Nullable
    public static VoiceResponse getMostRecent(@NonNull final ArrayList<VoiceResponse> voiceResponses){
        if(voiceResponses.isEmpty()){
            return null;
        }
        Collections.sort(voiceResponses,
                         (thisResponse, otherResponse) -> thisResponse.dateTime.compareTo(otherResponse.dateTime));
        return voiceResponses.get(0);
    }

    public static boolean hasSuccessful(@Nullable final VoiceResponse voiceResponse){
        return voiceResponse != null &&
                voiceResponse.result.equals(VoiceResponse.Result.OK);
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<VoiceResponse> provideUpdateObservable() {
        return apiService.getOnboardingVoiceResponse()
                         .map(SenseVoicePresenter::getMostRecent)
                         .doOnNext(this::updateFailCount);
    }

    @MainThread
    public void reset(){
        failCount = 0;
        voiceResponse.forget();
    }

    @MainThread
    public int getFailCount(){
        return failCount;
    }

    public void updateHasCompletedTutorial(final boolean hasCompleted){
        accountPresenter.latest()
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                account -> {
                                    preferences.setHasCompletedVoiceTutorial(
                                               account.getId(), hasCompleted);
                                    logEvent("completed tutorial = " + hasCompleted);
                                }, Functions.LOG_ERROR);
    }

    private void updateFailCount(@Nullable final VoiceResponse voiceResponse){
        if(!hasSuccessful(voiceResponse)){
            failCount++;
        }
        logEvent("failCount = " + failCount);
    }

}