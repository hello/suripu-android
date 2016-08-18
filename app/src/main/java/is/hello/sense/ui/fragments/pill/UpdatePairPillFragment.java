package is.hello.sense.ui.fragments.pill;

import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public class UpdatePairPillFragment extends BasePairPillFragment implements OnBackPressedInterceptor {

    @Override
    protected boolean wantsBackButton() {
        return false;
    }

    @Override
    protected void finishedPairing(final boolean success) {
        if (success) {
            LoadingDialogFragment.show(getFragmentManager(),
                                       null, LoadingDialogFragment.OPAQUE_BACKGROUND);
            getFragmentManager().executePendingTransactions();
            LoadingDialogFragment.closeWithMessageTransition(getFragmentManager(), this::finishFlow, R.string.sleep_pill_paired);
        } else {
            presentError(new Throwable());
        }

    }

    @Override
    protected void skipPairingPill() {
        finishFlowWithResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected int getTitleRes() {
        return R.string.update_pair_pill_title;
    }

    @Override
    protected int getSubTitleRes() {
        return R.string.update_pair_pill_sub_title;
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        return false;
    }
}
