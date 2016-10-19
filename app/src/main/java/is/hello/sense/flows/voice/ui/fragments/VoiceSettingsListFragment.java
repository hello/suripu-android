package is.hello.sense.flows.voice.ui.fragments;

import android.view.View;

import is.hello.sense.flows.voice.ui.views.VoiceSettingsListView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class VoiceSettingsListFragment extends PresenterFragment<VoiceSettingsListView> {

    public static final int RESULT_VOLUME_SELECTED = 99;

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new VoiceSettingsListView(getActivity());
            presenterView.setVolumeValueClickListener(this::redirectToVolumeSelection);
        }
    }

    private void redirectToVolumeSelection(final View ignore) {
        finishFlowWithResult(RESULT_VOLUME_SELECTED);
    }
}
