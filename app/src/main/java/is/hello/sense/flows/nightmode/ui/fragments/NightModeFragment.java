package is.hello.sense.flows.nightmode.ui.fragments;

import is.hello.sense.flows.nightmode.ui.views.NightModeView;
import is.hello.sense.mvp.presenters.PresenterFragment;

/**
 * Control night mode settings
 */

public class NightModeFragment extends PresenterFragment<NightModeView>
        implements NightModeView.Listener {
    @Override
    public void initializePresenterView() {
        if(presenterView == null) {
            presenterView = new NightModeView(getActivity());
            presenterView.setRadioGroupListener(this);
        }
    }

    @Override
    public void offModeSelected() {
        //todo
    }

    @Override
    public void onModeSelected() {
        //todo
    }

    @Override
    public void scheduledModeSelected() {
        //todo
    }
}
