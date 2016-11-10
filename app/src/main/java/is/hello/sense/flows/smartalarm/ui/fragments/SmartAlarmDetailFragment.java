package is.hello.sense.flows.smartalarm.ui.fragments;


import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.expansions.interactors.ExpansionsInteractor;
import is.hello.sense.flows.expansions.utils.ExpansionCategoryFormatter;
import is.hello.sense.flows.smartalarm.ui.views.SmartAlarmDetailView;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.SmartAlarmInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.util.DateFormatter;

public class SmartAlarmDetailFragment extends PresenterFragment<SmartAlarmDetailView> {
    @Inject
    DateFormatter dateFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    SmartAlarmInteractor smartAlarmInteractor;
    @Inject
    ExpansionsInteractor expansionsInteractor;
    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new SmartAlarmDetailView(getActivity(),
                                                     this::onTimeClicked,
                                                     this::onHelpClicked,
                                                     this::onToneClicked,
                                                     this::onRepeatClicked);
        }
    }
    //endregion

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
