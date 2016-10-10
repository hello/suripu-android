package is.hello.sense.presenters;

import android.support.annotation.NonNull;
import android.view.View;

import is.hello.sense.presenters.outputs.BaseOutput;

public class SenseUpdateIntroPresenter extends BasePresenter<SenseUpdateIntroPresenter.Output> {


    @Override
    public void onDetach() {
        // no interactors
    }

    @SuppressWarnings("unused")
    public void onPrimaryClicked(@NonNull final View clickedView) {
        view.finishFlow(); // todo replace with router
    }

    @SuppressWarnings("unused")
    public void onSecondaryClicked(@NonNull final View clickedView) {
        //todo track with correct event
        //Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_SENSE, null);
        view.showHelpUrl();
    }

    public interface Output extends BaseOutput {
        void showHelpUrl();

        void finishFlow();
    }
}
