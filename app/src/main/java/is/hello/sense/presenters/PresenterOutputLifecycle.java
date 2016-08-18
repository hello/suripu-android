package is.hello.sense.presenters;

public interface PresenterOutputLifecycle<T> {

    /**
     * @param view Bind reference to PresenterOutput
     */
    void setView(final T view);

    /**
     * Release reference to PresenterOutput
     */
    void onDestroyView();

    /**
     * Release reference to Intercepter
     */
    void onDestroy();
}
