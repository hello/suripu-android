package is.hello.sense.presenters;

import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.SenseResetOriginalInteractor;
import is.hello.sense.presenters.outputs.BaseOutput;
import is.hello.sense.ui.common.UserSupport;

public class SenseResetOriginalPresenter extends BasePresenter<SenseResetOriginalPresenter.Output> {

    private final SenseResetOriginalInteractor interactor;
    private Output view;

    public SenseResetOriginalPresenter(final SenseResetOriginalInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public void setView(@NonNull final Output view) {
        this.view = view;
    }

    @Override
    public void onDestroyView() {
        view = null;
    }

    @Override
    public void onDestroy() {
        interactor.destroy();
    }

    public void navigateToHelp(final Activity activity) {
        UserSupport.showForHelpStep(activity, UserSupport.HelpStep.RESET_ORIGINAL_SENSE);
    }

    public void startNetworkCall() {
        view.showProgress();
        interactor.update();
    }

    public InteractorSubject<Boolean> getInteractorSubject() {
        return interactor.resetResult;
    }

    public void onInteractorOutputNext(final Boolean resetSuccessful) {
        if (resetSuccessful) {
            view.onNetworkCallSuccess();
        }
        view.hideProgress();
    }

    public void onInteractorOutputError(final Throwable e) {
        view.onNetworkCallFailure(e);
        view.hideProgress();
    }

    public interface Output extends BaseOutput {
        void showProgress();

        void hideProgress();

        void onNetworkCallSuccess();

        void onNetworkCallFailure(Throwable e);
    }
}
