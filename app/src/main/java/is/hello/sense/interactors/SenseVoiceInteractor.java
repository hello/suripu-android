package is.hello.sense.interactors;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.VoiceResponse;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;
import rx.schedulers.Schedulers;

public class SenseVoiceInteractor extends ValueInteractor<VoiceResponse> {

    @Inject
    ApiService apiService;
    @Inject
    PersistentPreferencesInteractor preferences;
    @Inject
    AccountInteractor accountPresenter;

    @Inject public SenseVoiceInteractor(){}


    public final static long UPDATE_DELAY_SECONDS = 1;
    public final InteractorSubject<VoiceResponse> voiceResponse = this.subject;
    private int failCount = 0;
    private long lastValidResponseTime = System.currentTimeMillis();

    @Nullable
    public static VoiceResponse getMostRecent(@NonNull final ArrayList<VoiceResponse> voiceResponses){
        if(voiceResponses.isEmpty()){
            return null;
        }
        Collections.sort(voiceResponses,
                         (thisResponse, otherResponse) -> otherResponse.dateTime.compareTo(thisResponse.dateTime));
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
                         .map(SenseVoiceInteractor::getMostRecent)
                         .filter(this::isValidResponse)
                         .doOnNext(this::updateState);
    }

    public void updateState(@NonNull final VoiceResponse voiceResponse) {
        updateLastValidResponseTime(voiceResponse);
        updateFailCount(voiceResponse);
    }

    /**
     * @return true if response is not null and date time value is after last valid time
     */
    @VisibleForTesting
    public Boolean isValidResponse(@Nullable final VoiceResponse voiceResponse) {
        return voiceResponse != null &&
                voiceResponse.dateTime.isAfter(lastValidResponseTime);
    }

    @MainThread
    public void reset(){
        lastValidResponseTime = System.currentTimeMillis();
        failCount = 0;
        voiceResponse.forget();
    }

    @MainThread
    public int getFailCount(){
        return failCount;
    }

    @MainThread
    public long getLastValidResponseTime() {
        return lastValidResponseTime;
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

    private void updateLastValidResponseTime(@NonNull final VoiceResponse voiceResponse) {
        this.lastValidResponseTime = voiceResponse.dateTime.getMillis();
        logEvent("lastValidResponseTime = " + lastValidResponseTime);
    }
}
