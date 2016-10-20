package is.hello.sense.flows.voice.ui.fragments;

import android.view.View;

import is.hello.sense.flows.voice.ui.views.VoiceVolumeView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class VoiceVolumeFragment extends PresenterFragment<VoiceVolumeView> {

    @Override
    public void initializePresenterView() {
        if(presenterView == null){
            presenterView = new VoiceVolumeView(getActivity());
        }
        presenterView.setDoneButtonClickListener(this::postSelectedVolume);
    }

    private void postSelectedVolume(final View ignore) {
        //todo post to volume api
        final int value = presenterView.getVolume();
        finishFlow();
    }

}
