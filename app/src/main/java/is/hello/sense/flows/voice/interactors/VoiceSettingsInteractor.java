package is.hello.sense.flows.voice.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.Constants;
import rx.Observable;

public class VoiceSettingsInteractor extends ValueInteractor<SenseVoiceSettings> {

    public static final String EMPTY_ID = Constants.EMPTY_STRING;
    private static final int REPEAT_MAX_COUNT = 15;
    private static final int RESUBSCRIBE_DELAY_MILLIS = 1000;
    private static final String KEY_SENSE_ID = VoiceSettingsInteractor.class.getName() + "KEY_SENSE_ID";
    private final ApiService apiService;
    private String senseId = EMPTY_ID;
    private boolean changingVolume = false;

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

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        this.senseId = savedState.getString(KEY_SENSE_ID, EMPTY_ID);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle bundle = super.onSaveState();
        if(bundle == null){
            bundle = new Bundle();
        }
        bundle.putString(KEY_SENSE_ID, senseId);
        return bundle;
    }

    public Observable<SenseVoiceSettings> setMuted(final boolean muted){
        changingVolume = false;
        return getNewInstanceOfLatestSettings()
                .flatMap( newSettings -> {
                    newSettings.setMuted(muted);
                    return setAndPoll(newSettings);
                });
    }

    public Observable<SenseVoiceSettings> setPrimaryUser(final boolean isPrimaryUser){
        changingVolume = false;
        return getNewInstanceOfLatestSettings()
                .flatMap( newSettings -> {
                    newSettings.setPrimaryUser(isPrimaryUser);
                    return setAndPoll(newSettings);
                });
    }

    public Observable<SenseVoiceSettings> setVolume(final int volume){
        changingVolume = true;
        return getNewInstanceOfLatestSettings()
                .flatMap(newSettings -> {
                    newSettings.setVolume(volume);
                    return setAndPoll(newSettings);
                });
    }

    private Observable<SenseVoiceSettings> getNewInstanceOfLatestSettings() {
        return latest().map(SenseVoiceSettings::newInstance);
    }

    /**
     * Attempts to patch newSettings and retry polling for current settings until updated or failed
     * @return Observable Boolean true if new settings has updated {@link this#settingsSubject}
     */
    private Observable<SenseVoiceSettings> setAndPoll(@NonNull final SenseVoiceSettings newSettings){
        //todo handle error if update fails after repeat count max
        return apiService.setVoiceSettings(senseId, newSettings)
                         .flatMap( ignore -> provideUpdateObservable()
                                 .doOnNext( responseSettings -> {
                                     //updateVolumeTextView subject with new settings
                                     if (newSettings.equals(responseSettings)) {
                                         if (!changingVolume) {
                                             responseSettings.setVolume(settingsSubject.getValue().getVolume());
                                         }
                                         settingsSubject.onNext(responseSettings);
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

    private Boolean hasUpdatedTo(@NonNull final SenseVoiceSettings newSettings) {
        return settingsSubject.hasValue() && settingsSubject.getValue().equals(newSettings);
    }

    public void setSenseId(@NonNull final String id){
        this.senseId = id;
    }
}
