package is.hello.sense.mvp.view;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticFragmentAdapter;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.fragments.InsightsFragment;
import is.hello.sense.ui.fragments.RoomConditionsFragment;
import is.hello.sense.ui.fragments.TrendsFragment;
import is.hello.sense.ui.fragments.settings.AppSettingsFragment;
import is.hello.sense.ui.fragments.sounds.SoundsFragment;
import is.hello.sense.ui.widget.ExtendedViewPager;
import is.hello.sense.ui.widget.SelectorView;

public final class BacksideView extends PresenterView {
    public static final int ITEM_ROOM_CONDITIONS = 0;
    public static final int ITEM_TRENDS = 1;
    public static final int ITEM_INSIGHTS = 2;
    public static final int ITEM_SOUNDS = 3;
    public static final int ITEM_APP_SETTINGS = 4;
    public static final int OPTION_ANIMATE = (1 << 1);
    public static final int DEFAULT_START_ITEM = BacksideView.ITEM_INSIGHTS;

    private int tabSelectorHeight;
    private SelectorView tabSelector;
    private ExtendedViewPager pager;
    private StaticFragmentAdapter adapter;


    public BacksideView(@NonNull final Activity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public final View createView(@NonNull final LayoutInflater inflater, @NonNull final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_backside, container, false);
        final Resources resources = context.getResources();
        this.pager = (ExtendedViewPager) view.findViewById(R.id.fragment_backside_pager);

        this.tabSelectorHeight = resources.getDimensionPixelSize(R.dimen.action_bar_height);
        this.tabSelector = (SelectorView) view.findViewById(R.id.fragment_backside_tabs);
        tabSelector.setButtonLayoutParams(new SelectorView.LayoutParams(0, SelectorView.LayoutParams.MATCH_PARENT, 1));

        return view;
    }

    @Override
    public final void destroyView() {
        pager.clearOnPageChangeListeners();
    }

    public final void addOnPageChangeListener(@NonNull final ViewPager.OnPageChangeListener listener) {
        pager.addOnPageChangeListener(listener);
    }

    public final void setOnSelectionChangedListener(@NonNull final SelectorView.OnSelectionChangedListener listener) {
        tabSelector.setOnSelectionChangedListener(listener);
    }

    public final void setAdapter(@NonNull final FragmentManager fragmentManager) {
        this.adapter = new StaticFragmentAdapter(fragmentManager,
                                                 new StaticFragmentAdapter.Item(RoomConditionsFragment.class, getString(R.string.title_current_conditions)),
                                                 new StaticFragmentAdapter.Item(TrendsFragment.class, getString(R.string.title_trends)),
                                                 new StaticFragmentAdapter.Item(InsightsFragment.class, getString(R.string.action_insights)),
                                                 new StaticFragmentAdapter.Item(SoundsFragment.class, getString(R.string.action_alarm)),
                                                 new StaticFragmentAdapter.Item(AppSettingsFragment.class, getString(R.string.action_settings)));
        pager.setAdapter(adapter);
        final @DrawableRes int[] inactiveIcons = {
                R.drawable.backside_icon_currently,
                R.drawable.backside_icon_trends,
                R.drawable.backside_icon_insights,
                R.drawable.backside_icon_sounds,
                R.drawable.backside_icon_settings,
        };
        final @DrawableRes int[] activeIcons = {
                R.drawable.backside_icon_currently_active,
                R.drawable.backside_icon_trends_active,
                R.drawable.backside_icon_insights_active,
                R.drawable.backside_icon_sounds_active,
                R.drawable.backside_icon_settings_active,
        };
        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(i),
                                                                   inactiveIcons[i]);
            final SpannableString activeContent = createIconSpan(adapter.getPageTitle(i),
                                                                 activeIcons[i]);
            final ToggleButton button = tabSelector.addOption(activeContent, inactiveContent, false);
            button.setPadding(0, 0, 0, 0);
        }
        tabSelector.setSelectedIndex(pager.getCurrentItem());
    }

    public final void setCurrentItem(final int currentItem, final int options) {
        final boolean animate = ((options & OPTION_ANIMATE) == OPTION_ANIMATE);
        pager.setCurrentItem(currentItem, animate);
    }

    public final boolean isShowingAppSettings() {
        return pager.getCurrentItem() == BacksideView.ITEM_APP_SETTINGS;
    }

    public final boolean isShowingDefaultStartItem() {
        return pager.getCurrentItem() != DEFAULT_START_ITEM;
    }

    public final void setSelectedIndex(final int position) {
        tabSelector.setSelectedIndex(position);
    }

    @Nullable
    public final BacksideTabFragment getCurrentTabFragment(@NonNull final FragmentManager fragmentManager) {
        if (adapter != null) {
            // This depends on semi-undefined behavior. It may break in a future update
            // of the Android support library, but won't break if the host OS changes.
            return (BacksideTabFragment) fragmentManager.findFragmentByTag(getItemTag(pager.getCurrentItem()));
        } else {
            return null;
        }
    }

    public final void setChromeTranslationAmount(final float amount) {
        final float tabsTranslationY = Anime.interpolateFloats(amount, 0f, -tabSelectorHeight);
        tabSelector.setTranslationY(tabsTranslationY);

        final float buttonAlpha = Anime.interpolateFloats(amount, 1f, 0f);
        for (int i = 0, count = tabSelector.getButtonCount(); i < count; i++) {
            tabSelector.getButtonAt(i).setAlpha(buttonAlpha);
        }
    }

    public final float getChromeTranslationAmount() {
        return (-tabSelector.getTranslationY() / tabSelectorHeight);
    }

    public final void setHasUnreadInsightItems(final boolean hasUnreadInsightItems) {
        final @DrawableRes int iconRes = hasUnreadInsightItems
                ? R.drawable.backside_icon_insights_unread
                : R.drawable.backside_icon_insights;

        final ToggleButton button = tabSelector.getButtonAt(ITEM_INSIGHTS);
        final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(ITEM_INSIGHTS),
                                                               iconRes);
        button.setTextOff(inactiveContent);
        if (!button.isChecked()) {
            button.setText(inactiveContent);
        }
    }

    public final void setHasUnreadAccountItems(final boolean hasUnreadAccountItems) {
        final @DrawableRes int iconRes = hasUnreadAccountItems
                ? R.drawable.backside_icon_settings_unread
                : R.drawable.backside_icon_settings;

        final ToggleButton button = tabSelector.getButtonAt(ITEM_APP_SETTINGS);
        final SpannableString inactiveContent = createIconSpan(adapter.getPageTitle(ITEM_APP_SETTINGS),
                                                               iconRes);
        button.setTextOff(inactiveContent);
        if (!button.isChecked()) {
            button.setText(inactiveContent);
        }
    }

    private String getItemTag(int position) {
        if (position < 0 || position > adapter.getCount() - 1) {
            position = pager.getCurrentItem();
        }
        return "android:switcher:" + pager.getId() + ":" + adapter.getItemId(position);
    }

    private SpannableString createIconSpan(@NonNull final CharSequence title, @DrawableRes final int iconRes) {
        final ImageSpan image = new ImageSpan(context, iconRes);
        final SpannableString span = new SpannableString(title);
        span.setSpan(image, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

}
