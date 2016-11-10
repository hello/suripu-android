package is.hello.sense.flows.smartalarm.ui.fragments;


import android.view.View;

import is.hello.sense.flows.smartalarm.ui.views.SmartAlarmDetailView;
import is.hello.sense.mvp.presenters.PresenterFragment;

public class SmartAlarmDetailFragment extends PresenterFragment<SmartAlarmDetailView> {
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new SmartAlarmDetailView(getActivity(),
                                                     this::onTimeClicked,
                                                     this::onHelpClicked,
                                                     this::onToneClicked,
                                                     this::onRepeatClicked,
                                                     this::onDeleteClicked);
        }
    }

    //region methods
    private void onTimeClicked(final View ignored) {

    }

    private void onHelpClicked(final View ignored) {

    }

    private void onToneClicked(final View ignored) {

    }

    private void onRepeatClicked(final View ignored) {

    }

    private void onDeleteClicked(final View ignored) {

    }
    //endregion
}
