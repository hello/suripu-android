package is.hello.sense.flows.home.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.home.util.HomeViewPagerPresenterDelegate;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.mvp.fragments.ViewPagerSenseViewFragment;
import is.hello.sense.mvp.util.BaseViewPagerPresenterDelegate;
import is.hello.sense.ui.dialogs.InsightInfoFragment;

public class FeedSenseViewFragment extends ViewPagerSenseViewFragment
        implements InsightInfoFragment.ParentProvider {
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
                         this::bindVoiceSettings,
                         Functions.LOG_ERROR);
        hasVoiceInteractor.update();
    }
    //endRegion

    //region methods
    @VisibleForTesting
    public void bindVoiceSettings(final boolean hasVoice) {
        if (hasVoice) {
            senseView.unlockViewPager(this);
        } else {
            senseView.lockViewPager(getStartingItemPosition());
        }
    }

    @Nullable
    @Override
    public InsightInfoFragment.Parent provideInsightInfoParent() {
        final Fragment fragment = getCurrentFragment();
        if (fragment instanceof InsightInfoFragment.Parent) {
            return (InsightInfoFragment.Parent) fragment;
        } else {
            return null;
        }
    }

    //endRegion
}