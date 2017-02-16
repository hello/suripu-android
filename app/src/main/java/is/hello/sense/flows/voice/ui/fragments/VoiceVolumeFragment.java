package is.hello.sense.flows.voice.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.SenseVoiceSettings;
import is.hello.sense.flows.voice.interactors.VoiceSettingsInteractor;
import is.hello.sense.flows.voice.ui.views.VoiceVolumeView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.fragments.PresenterFragment;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class VoiceVolumeFragment extends PresenterFragment<VoiceVolumeView>
implements OnBackPressedInterceptor{

    @Inject
    VoiceSettingsInteractor voiceSettingsInteractor;

    private Subscription updateSettingsSubscription = Subscriptions.empty();

    @Override
    public void initializeSenseView() {
        if (senseView == null) {
            senseView = new VoiceVolumeView(getActivity());
        }
        senseView.setDoneButtonClickListener(this::postSelectedVolume);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(voiceSettingsInteractor);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBlockingActivity(false, null);
        bindAndSubscribe(voiceSettingsInteractor.settingsSubject,
                         this::bindSettings,
                         Functions.IGNORE_ERROR);
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        updateSettingsSubscription.unsubscribe();
    }

    public void bindSettings(@NonNull final SenseVoiceSettings settings) {
        this.senseView.setVolume(settings.getVolumeOrDefault());
    }

    private void postSelectedVolume(final View ignore) {
        final int volume = senseView.getVolume();
        updateSettings(voiceSettingsInteractor.setVolume(volume));
    }

    private void updateSettings(@NonNull final Observable<SenseVoiceSettings> updateObservable) {
        showLockedBlockingActivity(R.string.voice_settings_progress_updating);
        this.senseView.setVisibility(View.GONE);
        updateSettingsSubscription.unsubscribe();
        updateSettingsSubscription = bind(updateObservable)
                .subscribe(Functions.NO_OP,
                           this::presentError,
                           () -> hideBlockingActivity(true, stateSafeExecutor.bind(this::finishFlow))
                          );

    }

    private void presentError(@NonNull final Throwable e) {
        showProgress(false);
        if (e instanceof VoiceSettingsInteractor.SettingsUpdateThrowable) {
            final ErrorDialogFragment.PresenterBuilder builder = new ErrorDialogFragment.PresenterBuilder(e);
            builder.withTitle(StringRef.from(R.string.voice_settings_update_error_title));
            showErrorDialog(builder);
        } else {
            showErrorDialog(new ErrorDialogFragment.PresenterBuilder(e));
        }
    }

    private void showProgress(final boolean show) {
        if (show) {
            senseView.setVisibility(View.INVISIBLE);
            showBlockingActivity(null);
        } else {
            hideBlockingActivity(false, null);
            senseView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        stateSafeExecutor.execute(this::finishFlow);
        return true;
    }
}
