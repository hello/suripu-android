package is.hello.sense.presenters;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.view.View;

import is.hello.commonsense.bluetooth.errors.SenseNotFoundError;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.interactors.CurrentSenseInteractor;
import is.hello.sense.interactors.hardware.SenseResetOriginalInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class SenseResetOriginalPresenter
        extends BasePresenter<SenseResetOriginalPresenter.Output> {

    private final CurrentSenseInteractor currentSenseInteractor;
    private SenseResetOriginalInteractor interactor;

    public SenseResetOriginalPresenter(final SenseResetOriginalInteractor interactor,
                                       final CurrentSenseInteractor resetInteractor) {
        this.interactor = interactor;
        this.currentSenseInteractor = resetInteractor;
        addInteractor(interactor);
    }

    @Override
    public void onDetach() {
        interactor = null;
    }

    public void navigateToHelp(final Activity activity) {
        UserSupport.showForHelpStep(activity, UserSupport.HelpStep.RESET_ORIGINAL_SENSE);
    }

    public void startOperation() {
        showBlockingActivity(R.string.dialog_sense_reset_original);
        if(currentSenseInteractor.getCurrentSense() == null){
            onError(new SenseNotFoundError());
            return;
        }
        bindAndSubscribe(interactor.discoverPeripheralForDevice(currentSenseInteractor.getCurrentSense()),
                         ignore -> this.checkConnection(),
                         this::onError);
    }

    public void onOperationComplete(final Void ignored) {
        hideBlockingActivity(true, () -> {
            currentSenseInteractor.senseDevice.forget();
            interactor.reset();
            view.onOperationSuccess();
        });
    }

    public void onError(final Throwable e) {
        hideBlockingActivity(false, () -> {
            final ErrorDialogFragment.PresenterBuilder builder = ErrorDialogFragment.newInstance(e);
            builder.withMessage(StringRef.from(R.string.error_factory_reset_original_sense_message));
            view.showErrorDialog(builder);
            view.showRetry(R.string.action_skip,
                           ignored -> onOperationComplete(null)
            );
        });

    }

    private void checkConnection(){
        logEvent("checkConnection");
        if(interactor.isConnected()) {
            factoryResetOperation();
        } else {
            bindAndSubscribe(interactor.connectToPeripheral()
                                               .filter(ConnectProgress.CONNECTED::equals),
                             ignore -> this.factoryResetOperation(),
                             this::onError);
        }
    }

    private void factoryResetOperation() {
        bindAndSubscribe(interactor.unsafeFactoryReset(),
                         this::onOperationComplete,
                         this::onError);
    }

    public interface Output extends BaseOutput {

        void onOperationSuccess();

        void showRetry(@StringRes int retryRes, View.OnClickListener onClickListener);
    }
}
