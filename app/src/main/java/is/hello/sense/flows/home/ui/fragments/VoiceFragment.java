package is.hello.sense.flows.home.ui.fragments;

import is.hello.sense.flows.home.ui.views.VoiceView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class VoiceFragment extends PresenterFragment<VoiceView> {
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new VoiceView(getActivity());
        }
    }
}
