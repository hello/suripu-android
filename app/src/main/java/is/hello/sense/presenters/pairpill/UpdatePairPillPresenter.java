package is.hello.sense.presenters.pairpill;


import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;

import com.segment.analytics.Properties;

import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.presenters.pairpill.BasePairPillPresenter;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class UpdatePairPillPresenter extends BasePairPillPresenter {

    public UpdatePairPillPresenter(final HardwareInteractor hardwareInteractor) {
        super(hardwareInteractor
             );
    }



    @Override
    public void trackOnCreate() {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PAIR_PILL_IN_APP, null); //todo make new event
    }

    @Override
    public int getTitleRes() {
        return R.string.update_pair_pill_title;
    }

    @Override
    public int getSubTitleRes() {
        return R.string.update_pair_pill_sub_title;
    }

    @Override
    public void trackOnSkip() {
        final Properties properties =
                Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "pill_pairing");
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP_IN_APP, properties);//todo change

    }

    @Override
    public boolean showSkipButtonOnError() {
        return true;
    }

    @Override
    public boolean wantsBackButton() {
        return false;
    }

    @Override
    public void finishedPairingAction(@NonNull final Activity activity, final boolean success) {
        if (success) {
            LoadingDialogFragment.show(activity.getFragmentManager(),
                                       null, LoadingDialogFragment.OPAQUE_BACKGROUND);
            activity.getFragmentManager().executePendingTransactions();
            LoadingDialogFragment.closeWithMessageTransition(activity.getFragmentManager(), view::finishFlow, R.string.sleep_pill_paired);
        } else {
            this.presentError(new Throwable());
        }
    }

    @Override
    public void onHelpClick(@NonNull final View viewClicked) {
        view.showHelpUri(UserSupport.HelpStep.PILL_PAIRING);
    }


}
