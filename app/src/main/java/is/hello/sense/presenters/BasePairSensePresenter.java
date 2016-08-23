package is.hello.sense.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;

public abstract class BasePairSensePresenter extends ScopedPresenter<BasePairSensePresenter.Output> {


    protected static final String ARG_HAS_LINKED_ACCOUNT = "hasLinkedAccount";
    private final HardwarePresenter hardwarePresenter;
    private boolean linkedAccount = false;

    public BasePairSensePresenter(final HardwarePresenter hardwarePresenter){
        this.hardwarePresenter = hardwarePresenter;
    }

    @Override
    public void onDestroy() {

    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle state = new Bundle();
        state.putBoolean(ARG_HAS_LINKED_ACCOUNT, linkedAccount);
        return state;
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        linkedAccount = savedState.getBoolean(ARG_HAS_LINKED_ACCOUNT);
    }

    @StringRes
    public abstract int getTitleRes();

    @StringRes
    public abstract int getSubtitleRes();

    @StringRes
    public abstract int getPairingRes();

    @StringRes
    public abstract int getFinishedRes();

    public abstract String getOnCreateAnalyticsEvent();

    public abstract String getOnFinishAnalyticsEvent();

    protected abstract boolean shouldFinishFlow();

    protected abstract boolean shouldClearPeripheral();

    public boolean shouldShowPairDialog() {
        return false;
    }

    public void onPairSuccess(){
        if(shouldClearPeripheral()){
            hardwarePresenter.clearPeripheral();
        }
        if(shouldFinishFlow()){
            view.finishPairFlow();
        } else {
            view.finishActivity();
        }
    }

    public void checkLinkedAccount() {
        if(linkedAccount){
            view.finishUpOperations();
        } else {
            view.requestLinkAccount();
        }
    }

    public void updateLinkedAccount() {
        this.linkedAccount = true;
        view.finishUpOperations();
    }

    public boolean hasPeripheralPair() {
        Analytics.setSenseId(hardwarePresenter.getDeviceId());
        if (hardwarePresenter.getBondStatus() == GattPeripheral.BOND_BONDED) {
            view.showBlockingMessage(R.string.title_clearing_bond);
            return true;
        } else {
            view.showBlockingMessage(getPairingRes());
            return false;
        }
    }

    public boolean hasConnectivity(final ConnectProgress status) {
            if (status == ConnectProgress.CONNECTED) {
                view.showBlockingMessage(R.string.title_checking_connectivity);
                return true;
            } else {
                view.showBlockingMessage(Styles.getConnectStatusMessage(status));
                return false;
            }
    }

    public interface Output extends is.hello.sense.presenters.Output{

        void showBlockingMessage(@StringRes int blockingRes);

        void finishUpOperations();

        void requestLinkAccount();

        void finishPairFlow();

        void finishActivity();
    }
}
