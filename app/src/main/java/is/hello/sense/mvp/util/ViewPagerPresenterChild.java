package is.hello.sense.mvp.util;

public interface ViewPagerPresenterChild {

    void setUserVisibleHint(boolean isVisibleToUser);

    void onResume();

    void onPause();

    void onUserVisible();

    void onUserInvisible();

    void onViewInitialized();

    void releaseDelegateReference();
}
