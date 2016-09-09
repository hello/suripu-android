package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Analytics;

public class SenseUpgradeIntroPresenter extends BasePresenter<SenseUpgradeIntroPresenter.Output> {


    @Override
    public void onDetach() {
        // no interactors
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.trackEvent(Analytics.Upgrade.EVENT_UPGRADE_SENSE, null);
    }

    @SuppressWarnings("unused")
    public void onPrimaryClicked(@NonNull final View clickedView) {
        Analytics.trackEvent(Analytics.Upgrade.EVENT_SENSE_VOICE_START, null);
        execute(view::finishFlow); // todo replace with router
    }

    @SuppressWarnings("unused")
    public void onSecondaryClicked(@NonNull final View clickedView) {
        Analytics.trackEvent(Analytics.Upgrade.EVENT_PURCHASE_SENSE_VOICE, null);
        //todo replace with proper uri
        execute(() -> view.showHelpUri(UserSupport.ORDER_URL));
    }

    public interface Output extends BaseOutput {

        void finishFlow();
    }
}
