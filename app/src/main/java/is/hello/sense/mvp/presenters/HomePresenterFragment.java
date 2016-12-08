package is.hello.sense.mvp.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.home.util.HomeViewPagerPresenterDelegate;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;

//todo move to is.hello.sense.flows.home.ui.fragments and replace HomeFragment
public class HomePresenterFragment extends ViewPagerPresenterFragment {
    @Inject
    HasVoiceInteractor hasVoiceInteractor;

    //region ViewPagerPresenterFragment
    @NonNull
    @Override
    protected BaseViewPagerPresenterDelegate newViewPagerDelegateInstance() {
        return new HomeViewPagerPresenterDelegate(getResources());
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(hasVoiceInteractor);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(hasVoiceInteractor.hasVoice,
                         hasVoice -> {
                             if (hasVoice) {
                                 presenterView.createTabsAndPager(this);
                             } else {
                                 presenterView.hideTabsAfter(0);
                                 presenterView.lockViewPager(0);
                             }
                         },
                         Functions.LOG_ERROR);
        hasVoiceInteractor.update();
    }
    //endRegion
}