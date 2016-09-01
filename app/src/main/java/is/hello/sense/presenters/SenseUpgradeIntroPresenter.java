package is.hello.sense.presenters;

import android.support.annotation.NonNull;
import android.view.View;

import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;

public class SenseUpgradeIntroPresenter extends BasePresenter<SenseUpgradeIntroPresenter.Output> {


    @Override
    public void onDetach() {
        // no interactors
    }

    @SuppressWarnings("unused")
    public void onPrimaryClicked(@NonNull final View clickedView) {
        execute(()-> view.finishFlow()); // todo replace with router
    }

    @SuppressWarnings("unused")
    public void onSecondaryClicked(@NonNull final View clickedView) {
        //todo replace with proper uri
        execute( () -> view.showHelpUri(UserSupport.ORDER_URL));
    }

    public interface Output extends BaseOutput {

        void finishFlow();
    }
}
