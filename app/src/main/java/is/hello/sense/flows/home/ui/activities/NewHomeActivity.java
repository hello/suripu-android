package is.hello.sense.flows.home.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.ArrayMap;
import android.widget.ToggleButton;

import com.zendesk.logger.Logger;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.fragments.AppSettingsFragment;
import is.hello.sense.flows.home.ui.fragments.RoomConditionsFragment;
import is.hello.sense.flows.home.ui.fragments.SoundsFragment;
import is.hello.sense.flows.home.ui.fragments.TimelinePagerFragment;
import is.hello.sense.flows.home.ui.fragments.TrendsFragment;
import is.hello.sense.flows.home.ui.fragments.VoiceFragment;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationDelegate;
import is.hello.sense.ui.common.ScopedInjectionActivity;
import is.hello.sense.ui.widget.SelectorView;
import rx.functions.Func0;

/**
 * Will eventually replace {@link HomeActivity}
 */

public class NewHomeActivity extends ScopedInjectionActivity
        implements SelectorView.OnSelectionChangedListener,
        FragmentNavigation{

    private static final String KEY_CURRENT_ITEM_INDEX = NewHomeActivity.class.getSimpleName() + "CURRENT_ITEM_INDEX";
    private static final int DEFAULT_ITEM_INDEX = 2;
    private SelectorView bottomSelectorView;
    private FragmentNavigationDelegate fragmentNavigationDelegate;
    private final FragmentMapper fragmentMapper = new FragmentMapper();
    private int currentItemIndex;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_home);
        restoreState(savedInstanceState);
        this.fragmentNavigationDelegate = new FragmentNavigationDelegate(this,
                                                                         R.id.activity_new_home_backside_container,
                                                                         stateSafeExecutor);
        if(savedInstanceState != null) {
            this.fragmentNavigationDelegate.onRestoreInstanceState(savedInstanceState);
        }
        this.bottomSelectorView = (SelectorView) findViewById(R.id.activity_new_home_bottom_selector_view);
        initSelector(bottomSelectorView);

    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_ITEM_INDEX, currentItemIndex);
        if(fragmentNavigationDelegate != null){
            fragmentNavigationDelegate.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(this.bottomSelectorView != null){
            this.bottomSelectorView.setOnSelectionChangedListener(null);
        }
        if(fragmentNavigationDelegate != null) {
            fragmentNavigationDelegate.onDestroy();
        }
    }

    @Override
    public void onSelectionChanged(final int newSelectionIndex) {

        setCurrentItemIndex(newSelectionIndex);

        final String tag = (String) this.bottomSelectorView.getButtonTagAt(newSelectionIndex);
        final Fragment fragment = getFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment != getTopFragment() && !fragment.isRemoving()) {
            pushFragment(fragment, null, false); //todo save most recent 3 fragments in backstack
        } else if (fragment == null || fragment.isRemoving()) {
            pushFragment(fragmentMapper.getFragmentFromTag(tag), null, false);
        } else {
            Logger.d(NewHomeActivity.class.getName(), fragment + " is already visible");
        }
    }

    private void restoreState(@Nullable final Bundle savedInstanceState) {
        if(savedInstanceState != null){
            this.currentItemIndex = savedInstanceState.getInt(KEY_CURRENT_ITEM_INDEX, DEFAULT_ITEM_INDEX);
        } else {
            this.currentItemIndex = DEFAULT_ITEM_INDEX;
        }
    }

    @Override
    public void pushFragment(@NonNull final Fragment fragment,
                             @Nullable final String title,
                             final boolean wantsBackStackEntry) {
        this.fragmentNavigationDelegate.pushFragment(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void pushFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                              @Nullable final String title,
                                              final boolean wantsBackStackEntry) {
        this.fragmentNavigationDelegate.pushFragmentAllowingStateLoss(fragment, title, wantsBackStackEntry);
    }

    @Override
    public void popFragment(@NonNull final Fragment fragment, final boolean immediate) {
        this.fragmentNavigationDelegate.popFragment(fragment, immediate);
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent result) {
        //todo
    }

    @Nullable
    @Override
    public Fragment getTopFragment() {
        return fragmentNavigationDelegate.getTopFragment();
    }

    private void initSelector(@NonNull final SelectorView selectorView) {
        selectorView.setButtonLayoutParams(new SelectorView.LayoutParams(0, SelectorView.LayoutParams.MATCH_PARENT, 1));
        //todo update icons and order
        final @DrawableRes int[] inactiveIcons = {
                R.drawable.backside_icon_currently,
                R.drawable.icon_trends_24,
                R.drawable.icon_insight_24,
                R.drawable.icon_sound_24,
                R.drawable.icon_sense_24,
        };
        final @DrawableRes int[] activeIcons = {
                R.drawable.backside_icon_currently_active,
                R.drawable.icon_trends_active_24,
                R.drawable.icon_insight_active_24,
                R.drawable.icon_sound_active_24,
                R.drawable.icon_sense_active_24,
        };

        for (int i = 0; i < inactiveIcons.length; i++) {
            final SpannableString inactiveContent = createIconSpan("empty",
                                                                   inactiveIcons[i]);
            final SpannableString activeContent = createIconSpan("empty",
                                                                 activeIcons[i]);
            final ToggleButton button = selectorView.addOption(activeContent,
                                                               inactiveContent,
                                                               false);
            button.setPadding(0, 0, 0, 0);
        }
        selectorView.setButtonTags((Object[]) fragmentMapper.tags);
        selectorView.setOnSelectionChangedListener(this);
        selectorView.setSelectedIndex(getCurrentItemIndex());
        onSelectionChanged(getCurrentItemIndex());
    }

    private SpannableString createIconSpan(@NonNull final String title,
                                           @DrawableRes final int icon) {
        final SpannableString spannableString = new SpannableString(title);
        final ImageSpan imageSpan = new ImageSpan(this, icon);
        spannableString.setSpan(imageSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    private int getCurrentItemIndex() {
        return this.currentItemIndex;
    }

    public void setCurrentItemIndex(final int currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
    }

    private static class FragmentMapper {

        //todo these are the tags FragmentNavigationDelegate uses when making transactions
        // heavy dependence on fragment.class.getSimpleName()
        private final String TIMELINE_TAG = TimelinePagerFragment.class.getSimpleName();
        private final String TRENDS_TAG = TrendsFragment.class.getSimpleName();
        private final String HOME_TAG = VoiceFragment.class.getSimpleName();
        private final String SOUNDS_TAG = SoundsFragment.class.getSimpleName();
        private final String CONDITIONS_TAG = AppSettingsFragment.class.getSimpleName();

        final String[] tags = {
                TIMELINE_TAG,
                TRENDS_TAG,
                HOME_TAG,
                SOUNDS_TAG,
                CONDITIONS_TAG
        };

        private final ArrayMap<String, Func0<Fragment>> map;

        FragmentMapper(){
            this.map = new ArrayMap<>(tags.length);
            map.put(TIMELINE_TAG, TimelinePagerFragment::new);
            map.put(TRENDS_TAG, RoomConditionsFragment::new);
            map.put(SOUNDS_TAG, SoundsFragment::new);
            map.put(CONDITIONS_TAG, AppSettingsFragment::new);
            map.put(HOME_TAG, VoiceFragment::new);
        }

        Fragment getFragmentFromTag(@NonNull final String tag){
            if(map.containsKey(tag)){
                return map.get(tag)
                          .call();
            } else {
                throw new IllegalStateException("no fragment mapped to tag " + tag);
            }
        }

    }
}
