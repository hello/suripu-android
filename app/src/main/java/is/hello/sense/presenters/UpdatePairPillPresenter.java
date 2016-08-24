package is.hello.sense.presenters;


import android.app.Activity;
import android.support.annotation.NonNull;

import com.segment.analytics.Properties;

import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.util.Analytics;

public class UpdatePairPillPresenter extends BasePairPillPresenter {

    public UpdatePairPillPresenter(final HardwareInteractor hardwareInteractor,
                                   final UserFeaturesInteractor userFeaturesInteractor) {
        super(hardwareInteractor,
              userFeaturesInteractor);
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
     /*  todo use this
      if (success) {
            LoadingDialogFragment.show(activity.getFragmentManager(),
                                       null, LoadingDialogFragment.OPAQUE_BACKGROUND);
            activity.getFragmentManager().executePendingTransactions();
            LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(), this::finishFlow, R.string.sleep_pill_paired);
        } else {
            presentError(new Throwable());
        }*/
    }


}
