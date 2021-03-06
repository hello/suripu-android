package is.hello.sense.presenters;


import android.support.annotation.NonNull;
import android.view.View;

import is.hello.sense.presenters.outputs.BaseOutput;

public class SenseUpgradeReadyPresenter extends BasePresenter<SenseUpgradeReadyPresenter.Output> {

    @Override
    public void onDetach() {
        // no interactors
    }

    @SuppressWarnings("unused")
    public void onPrimaryClick(@NonNull final View clickedView) {
        //todo use router
        execute( () -> view.finishFlow());
    }


    public interface Output extends BaseOutput {
        void finishFlow(); //todo replace with router.
    }
}
