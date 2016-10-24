package is.hello.sense.flows.voice.interactors;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class VoiceSettingsInteractor extends ValueInteractor<SenseVoiceSettings> {

    public static final String EMPTY_ID = "";
    private static final int REPEAT_MAX_COUNT = 15;
    private static final int RESUBSCRIBE_DELAY_MILLIS = 1000;
    private final ApiService apiService;
    private String senseId = EMPTY_ID;

    public InteractorSubject<SenseVoiceSettings> settingsSubject = this.subject;

    public VoiceSettingsInteractor(@NonNull final ApiService apiService){
        this.apiService = apiService;
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
    protected Observable<SenseVoiceSettings> provideUpdateObservable() {
        if(EMPTY_ID.equals(senseId)){
            return Observable.error(new SenseNotFoundError()); //todo handle better
        }
        return apiService.getVoiceSettings(senseId);
    }

    public Observable<SenseVoiceSettings> setAndPoll(@NonNull final SenseVoiceSettings newSettings){

        return apiService.setVoiceSettings(senseId, newSettings)
                         .flatMap( ignore -> provideUpdateObservable()
                                 .doOnNext( responseSettings -> {
                                    if(newSettings.equals(responseSettings)){
                                        settingsSubject.onNext(responseSettings); //updateVolumeTextView subject with new settings
                                    }
                                 })
                                 .repeatWhen( onComplete -> onComplete.zipWith(
                                         Observable.range(1, REPEAT_MAX_COUNT)
                                         , (ignore2, integer) -> integer)
                                            .takeUntil( integer -> hasUpdatedTo(newSettings))
                                            .flatMap( repeatCount -> Observable.timer(RESUBSCRIBE_DELAY_MILLIS, TimeUnit.MILLISECONDS))
                                            )
                                 );
    }

    public Boolean hasUpdatedTo(@NonNull final SenseVoiceSettings newSettings) {
        return settingsSubject.hasValue() && settingsSubject.getValue().equals(newSettings);
    }

    public void setSenseId(@NonNull final String id){
        this.senseId = id;
    }
}
