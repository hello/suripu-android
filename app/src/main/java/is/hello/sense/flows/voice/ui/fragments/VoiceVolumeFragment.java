package is.hello.sense.flows.voice.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.flows.voice.ui.views.VoiceVolumeView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class VoiceVolumeFragment extends PresenterFragment<VoiceVolumeView> {

    @Inject
    VoiceSettingsInteractor voiceSettingsInteractor;

    private Subscription updateSettingsSubscription = Subscriptions.empty();

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new VoiceVolumeView(getActivity());
        }
        presenterView.setDoneButtonClickListener(this::postSelectedVolume);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(voiceSettingsInteractor);
        bindAndSubscribe(voiceSettingsInteractor.settingsSubject,
                         this::bindSettings,
                         this::presentError);
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        updateSettingsSubscription.unsubscribe();
    }

    public void bindSettings(@NonNull final SenseVoiceSettings settings) {
        this.presenterView.setVolume(settings.getVolume());
    }

    private void postSelectedVolume(final View ignore) {
        final int volume = presenterView.getVolume();
        updateSettings(voiceSettingsInteractor.setVolume(volume));
    }

    private void updateSettings(@NonNull final Observable<SenseVoiceSettings> updateObservable) {
        showBlockingActivity(R.string.voice_settings_progress_updating); //todo use real copy
        this.presenterView.setVisibility(View.INVISIBLE);
        updateSettingsSubscription.unsubscribe();
        updateSettingsSubscription = bind(updateObservable)
                .subscribe(Functions.NO_OP,
                           this::presentError,
                           () -> hideBlockingActivity(true, this::finishFlow)
                          );

    }

    private void presentError(@NonNull final Throwable e) {
        this.presenterView.setVisibility(View.VISIBLE);
        hideBlockingActivity(false, null);
        showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e));
    }

}
