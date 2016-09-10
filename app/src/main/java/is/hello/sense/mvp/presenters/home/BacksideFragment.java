package is.hello.sense.mvp.presenters.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.UnreadStateInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.mvp.view.home.BacksideView;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.widget.SelectorView;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.InternalPrefManager;


public final class BacksideFragment extends PresenterFragment<BacksideView>
        implements
        ViewPager.OnPageChangeListener,
        SelectorView.OnSelectionChangedListener {
    public static final int OPTION_NONE = 0;

    private SharedPreferences internalPreferences;
    @Inject
    UnreadStateInteractor unreadStateInteractor;

    @Inject
    AccountInteractor accountInteractor;


    private boolean suppressNextSwipeEvent = false;
    private int lastState = ViewPager.SCROLL_STATE_IDLE;


    @Override
    public final BacksideView getPresenterView() {
        if (presenterView == null) {
            return new BacksideView(getActivity());
        }
        return presenterView;
    }

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_SHOWN, null);
        }
        this.internalPreferences = InternalPrefManager.getInternalPrefs(getActivity());
    }

    @Override
    public final void onResume() {
        super.onResume();
        final BacksideTabFragment fragment = presenterView.getCurrentTabFragment(getFragmentManager());
        if (fragment != null) {
            fragment.onUpdate();
        }
        accountInteractor.update();
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.setAdapter(getFragmentManager());
        presenterView.addOnPageChangeListener(this);
        presenterView.setOnSelectionChangedListener(this);
        final long itemLastUpdated =
                internalPreferences.getLong(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM_LAST_UPDATED, 0);
        if ((System.currentTimeMillis() - itemLastUpdated) <= Constants.STALE_INTERVAL_MS) {
            final int currentItem =
                    internalPreferences.getInt(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM, 0);
            presenterView.setCurrentItem(currentItem, OPTION_NONE);
        } else {
            presenterView.setCurrentItem(BacksideView.DEFAULT_START_ITEM, OPTION_NONE);
        }
        bindAndSubscribe(unreadStateInteractor.hasUnreadItems,
                         presenterView::setHasUnreadInsightItems,
                         Functions.LOG_ERROR);

        bindAndSubscribe(accountInteractor.account,
                         (a) -> {
                             if (presenterView.isShowingAppSettings()
                                     && Tutorial.TAP_NAME.shouldShow(getActivity())
                                     && a.getCreated().isBefore(Constants.RELEASE_DATE_FOR_LAST_NAME)) {
                                 presenterView.setHasUnreadAccountItems(true);
                             } else {
                                 presenterView.setHasUnreadAccountItems(false);
                             }
                         },
                         Functions.LOG_ERROR);
        accountInteractor.update();
    }

    public final boolean onBackPressed() {
        if (presenterView.isShowingDefaultStartItem()) {
            presenterView.setCurrentItem(BacksideView.DEFAULT_START_ITEM, BacksideView.OPTION_ANIMATE);
            return true;
        } else {
            return false;
        }
    }

    public final void saveCurrentItem(final int currentItem) {
        internalPreferences.edit()
                           .putInt(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM, currentItem)
                           .putLong(Constants.INTERNAL_PREF_BACKSIDE_CURRENT_ITEM_LAST_UPDATED,
                                    System.currentTimeMillis())
                           .apply();
    }

    @Override
    public final void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
    }

    @Override
    public final void onPageSelected(final int position) {
        presenterView.setSelectedIndex(position);
        saveCurrentItem(position);
    }

    @Override
    public final void onPageScrollStateChanged(final int state) {
        if (lastState == ViewPager.SCROLL_STATE_IDLE &&
                state != ViewPager.SCROLL_STATE_IDLE) {
            getAnimatorContext().beginAnimation("Backside swipe");
        } else if (lastState != ViewPager.SCROLL_STATE_IDLE &&
                state == ViewPager.SCROLL_STATE_IDLE) {
            if (suppressNextSwipeEvent) {
                this.suppressNextSwipeEvent = false;
            } else {
                Analytics.trackEvent(Analytics.Backside.EVENT_TAB_SWIPED, null);
            }
            getAnimatorContext().endAnimation("Backside swipe");
        }

        this.lastState = state;
    }

    @Override
    public final void onSelectionChanged(final int newSelectionIndex) {
        Analytics.trackEvent(Analytics.Backside.EVENT_TAB_TAPPED, null);
        this.suppressNextSwipeEvent = true;

        presenterView.setCurrentItem(newSelectionIndex, BacksideView.OPTION_ANIMATE);
    }

    public final void setCurrentItem(final int item, final int animateOptions) {
        presenterView.setCurrentItem(item, animateOptions);
    }

    public final BacksideTabFragment getCurrentTabFragment() {
        return presenterView.getCurrentTabFragment(getFragmentManager());
    }


}
