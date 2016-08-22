package is.hello.sense.presenters;

import android.support.annotation.StringRes;

import is.hello.sense.graph.presenters.HardwarePresenter;

public abstract class BasePairSensePresenter implements PresenterOutputLifecycle<BasePairSensePresenter.Output>{


    private final HardwarePresenter hardwarePresenter;
    private Output view;

    public BasePairSensePresenter(final HardwarePresenter hardwarePresenter){
        this.hardwarePresenter = hardwarePresenter;
    }

    @Override
    public void setView(final Output view) {
        this.view = view;
    }

    @Override
    public void onDestroyView() {
        view = null;
    }

    @Override
    public void onDestroy() {
        hardwarePresenter.clearPeripheral();
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

    public interface Output {

    }
}
