package is.hello.sense.flows.voice.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.Constants;
import rx.Observable;

public class VoiceSettingsInteractor extends ValueInteractor<SenseVoiceSettings> {

    public static final String EMPTY_ID = Constants.EMPTY_STRING;
    private static final int REPEAT_MAX_COUNT = 10;
    private static final int RESUBSCRIBE_DELAY_MILLIS = 1000;
    private static final String KEY_SENSE_ID = VoiceSettingsInteractor.class.getName() + "KEY_SENSE_ID";
    private final ApiService apiService;
    private String senseId = EMPTY_ID;

    public InteractorSubject<SenseVoiceSettings> settingsSubject = this.subject;

    @Inject
    public VoiceSettingsInteractor(@NonNull final ApiService apiService) {
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
        if (EMPTY_ID.equals(senseId)) {
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
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString(KEY_SENSE_ID, senseId);
        return bundle;
    }

    public Observable<SenseVoiceSettings> setMuted(final boolean muted) {
        return setAndPoll(new SenseVoiceSettings(null, muted, null));
    }

    public Observable<SenseVoiceSettings> setPrimaryUser(final boolean isPrimaryUser) {
        return setAndPoll(new SenseVoiceSettings(null, null, isPrimaryUser));
    }

    public Observable<SenseVoiceSettings> setVolume(final int volume) {
        return setAndPoll(new SenseVoiceSettings(volume, null, null));
    }

    /**
     * Attempts to patch newSettings and retry polling for current settings until updated or failed
     *
     * @return Observable Boolean true if new settings has updated {@link this#settingsSubject}
     */
    private Observable<SenseVoiceSettings> setAndPoll(@NonNull final SenseVoiceSettings newSettings) {
        if (EMPTY_ID.equals(senseId)) {
            return Observable.error(new SenseNotFoundError()); //todo handle better
        }
        return apiService.setVoiceSettings(senseId, newSettings)
                         .flatMap(ignore -> provideUpdateObservable()
                                 .doOnNext(responseSettings -> {
                                     if (newSettings.equals(responseSettings)) {
                                         settingsSubject.onNext(responseSettings);
                                     }
                                 })
                                 .repeatWhen(onComplete -> onComplete.zipWith(
                                         Observable.range(1, REPEAT_MAX_COUNT)
                                         , (ignore2, integer) -> integer)
                                                                     .takeUntil(integer -> hasUpdatedTo(newSettings))
                                                                     .flatMap( count -> {
                                                                         if (count >= REPEAT_MAX_COUNT && !hasUpdatedTo(newSettings)) {
                                                                             if (newSettings.getVolume() != null) {
                                                                                 return Observable.error(new SettingsUpdateThrowable());
                                                                             }
                                                                             else if (newSettings.isMuted() != null) {
                                                                                 return Observable.error(new MuteUpdateThrowable());
                                                                             }
                                                                             else if (newSettings.isPrimaryUser() != null) {
                                                                                 return Observable.error(new PrimaryUpdateThrowable());
                                                                             } else {
                                                                                 return Observable.just(count);
                                                                             }
                                                                         } else {
                                                                             return Observable.just(count);
                                                                         }
                                                                     })
                                                                     .flatMap(repeatCount -> Observable.timer(RESUBSCRIBE_DELAY_MILLIS, TimeUnit.MILLISECONDS))
                                                                     ));
    }

    private Boolean hasUpdatedTo(@NonNull final SenseVoiceSettings newSettings) {
        return settingsSubject.hasValue() && settingsSubject.getValue().equals(newSettings);
    }

    public void setSenseId(@NonNull final String id) {
        this.senseId = id;
    }

    public static class SettingsUpdateThrowable extends Throwable {

    }

    public static class MuteUpdateThrowable extends SettingsUpdateThrowable {

    }

    public static class PrimaryUpdateThrowable extends SettingsUpdateThrowable {
    }
}
