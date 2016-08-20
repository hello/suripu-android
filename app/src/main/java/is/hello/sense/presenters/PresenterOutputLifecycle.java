package is.hello.sense.presenters;

import is.hello.sense.presenters.outputs.BaseOutput;

public interface PresenterOutputLifecycle<T extends BaseOutput> {

    /**
     * @param view Bind reference to PresenterOutput
     */
    void setView(final T view);

    /**
     * Release reference to PresenterOutput
     */
    void onDestroyView();

    /**
     * Release reference to Interceptor
     */
    void onDestroy();

}
