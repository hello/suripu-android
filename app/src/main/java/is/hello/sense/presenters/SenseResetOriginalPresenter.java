package is.hello.sense.presenters;

import android.app.Activity;
import android.support.annotation.StringRes;

import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.interactors.SenseResetOriginalInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class SenseResetOriginalPresenter
        extends BaseHardwarePresenter<SenseResetOriginalPresenter.Output> {

    private final SenseResetOriginalInteractor resetInteractor;

    public SenseResetOriginalPresenter(final HardwareInteractor interactor,
                                       final SenseResetOriginalInteractor resetInteractor) {
        super(interactor);
        this.resetInteractor = resetInteractor;
    }

    public void navigateToHelp(final Activity activity) {
        UserSupport.showForHelpStep(activity, UserSupport.HelpStep.RESET_ORIGINAL_SENSE);
    }

    public void startOperation() {
        showBlockingActivity(R.string.dialog_sense_reset_original);
        //todo its possible that resetInteractor returns null
        bindAndSubscribe(hardwareInteractor.discoverPeripheralForDevice(resetInteractor.getCurrentSense()),
                         ignore -> this.checkConnection(),
                         this::onError);
    }

    public void onOperationComplete(final Void ignored) {
        hideBlockingActivity(true, () -> {
            resetInteractor.senseDevice.forget();
            view.onOperationSuccess();
        });
    }

    public void onError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
            builder.withMessage(StringRef.from(R.string.error_factory_reset_original_sense_message));
            view.showErrorDialog(builder);
            view.showRetry(R.string.action_skip);
        });

    }

    private void checkConnection(){
        logEvent("checkConnection");
        if(hardwareInteractor.isConnected()) {
            factoryResetOperation();
        } else {
            bindAndSubscribe(hardwareInteractor.connectToPeripheral()
                                               .filter(ConnectProgress.CONNECTED::equals),
                             ignore -> this.factoryResetOperation(),
                             this::onError);
        }
    }

    private void factoryResetOperation() {
        bindAndSubscribe(hardwareInteractor.unsafeFactoryReset(),
                         this::onOperationComplete,
                         this::onError);
    }

    public interface Output extends BaseOutput {

        void onOperationSuccess();

        void showRetry(@StringRes int retryRes);
    }
}
