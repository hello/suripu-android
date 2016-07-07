package is.hello.sense.ui.fragments.pill;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;

public class PillHardwareFragment extends InjectionFragment {

    @Inject
    DevicesPresenter devicesPresenter;

    private LoadingDialogFragment loadingDialogFragment;

    protected void showBlockingActivity(@StringRes final int titleRes) {
        if (loadingDialogFragment == null) {
            stateSafeExecutor.execute(() -> {
                this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(),
                                                                        getString(titleRes),
                                                                        LoadingDialogFragment.OPAQUE_BACKGROUND);
            });
        } else {
            loadingDialogFragment.setTitle(getString(titleRes));
        }
    }

    protected void hideBlockingActivity(final boolean success, @NonNull final Runnable onCompletion) {
        stateSafeExecutor.execute(() -> {
            if (success) {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                    this.loadingDialogFragment = null;
                    stateSafeExecutor.execute(onCompletion);
                });
            } else {
                LoadingDialogFragment.close(getFragmentManager());
                this.loadingDialogFragment = null;
                onCompletion.run();
            }
        });
    }

    protected void presentPillBatteryError(){
        final String helpUriString = UserSupport.DeviceIssue.SLEEP_PILL_LOW_BATTERY.getUri().toString();
        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                .withOperation("Check Pill Battery")
                .withTitle(R.string.dialog_title_replace_sleep_pill_battery)
                .withMessage(StringRef.from(R.string.dialog_message_replace_sleep_pill_battery))
                .withAction(helpUriString, R.string.label_having_trouble)
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

}
