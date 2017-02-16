package is.hello.sense.flows.settings.ui.fragments;

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.flows.settings.ui.views.AppSettingsView;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.mvp.fragments.PresenterFragment;
import is.hello.sense.util.Analytics;

public class AppSettingsFragment extends PresenterFragment<AppSettingsView> implements
        AppSettingsView.Listener {

    @Inject
    HasVoiceInteractor hasVoiceInteractor;

    public static AppSettingsFragment newInstance() {
        return new AppSettingsFragment();
    }

    @Override
    public final void initializeSenseView() {
        if (this.senseView == null) {
            this.senseView = new AppSettingsView(getActivity());
            this.senseView.setListener(this);
        }
    }

    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_SETTINGS, null);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(this.hasVoiceInteractor);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(this.hasVoiceInteractor.hasVoice,
                         this.senseView::showVoiceEnabledRows,
                         e -> this.senseView.showVoiceEnabledRows(false));


        this.senseView.setDebugText(getString(R.string.app_version_fmt,
                                              getString(R.string.app_name),
                                              BuildConfig.VERSION_NAME));
        this.senseView.enableDebug(BuildConfig.DEBUG_SCREEN_ENABLED);
        this.hasVoiceInteractor.update();
    }

    @Override
    public void onItemClicked(final int position) {
        finishFlowWithResult(position);
    }
}
