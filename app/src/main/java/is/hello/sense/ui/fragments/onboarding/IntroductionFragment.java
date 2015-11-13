package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.segment.analytics.Properties;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.StatusBarColorProvider;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.VideoPlayerActivity;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.OnViewPagerChangeAdapter;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SafeOnClickListener;

public class IntroductionFragment extends SenseFragment
        implements StatusBarColorProvider, OnBackPressedInterceptor,
        OnViewPagerChangeAdapter.Listener {
    public static final int RESPONSE_SIGN_IN = 0;
    public static final int RESPONSE_GET_STARTED = 1;

    private static final int INTRO_POSITION = 0;
    private static final int DRAWABLE_ON_SCREEN = 0;
    private static final int DRAWABLE_OFF_SCREEN = 1;
    private static final @DrawableRes int[] DIAGRAMS = {
            R.color.transparent,
            R.drawable.onboarding_intro_feature_alarm,
            R.drawable.onboarding_intro_feature_timeline,
            R.drawable.onboarding_intro_feature_sleep_score,
            R.drawable.onboarding_intro_feature_conditions,
            R.color.transparent,
    };

    private ImageView diagramImage;
    private LayerDrawable diagramLayers;

    private ViewPager viewPager;
    private PageDots pageDots;
    private OnViewPagerChangeAdapter onViewPagerChangeAdapter;

    private Button signInButton;
    private LinearLayout.LayoutParams signInLayoutParams;
    private View buttonDivider;
    private Button getStartedButton;

    private Window window;
    private @ColorInt int introStatusBarColor;
    private @ColorInt int featureStatusBarColor;

    private int lastSelectedPage = INTRO_POSITION;
    private boolean statusBarChanging = false;


    //region Lifecycle

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.window = activity.getWindow();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.lastSelectedPage = savedInstanceState.getInt("lastSelectedPage", INTRO_POSITION);
        }

        final Resources resources = getResources();
        final Drawable onScreen = ResourcesCompat.getDrawable(resources,
                                                              DIAGRAMS[0],
                                                              null);
        final Drawable offScreen = ResourcesCompat.getDrawable(resources,
                                                               DIAGRAMS[1],
                                                               null);
        //noinspection ConstantConditions
        offScreen.setAlpha(0);
        final Drawable[] layers = { onScreen, offScreen };
        this.diagramLayers = new LayerDrawable(layers);
        diagramLayers.setId(DRAWABLE_ON_SCREEN, DRAWABLE_ON_SCREEN);
        diagramLayers.setId(DRAWABLE_OFF_SCREEN, DRAWABLE_OFF_SCREEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_introduction, container, false);

        this.diagramImage = (ImageView) view.findViewById(R.id.fragment_onboarding_introduction_diagram);
        diagramImage.setImageDrawable(diagramLayers);

        this.viewPager = (ViewPager) view.findViewById(R.id.fragment_onboarding_introduction_pager);

        this.onViewPagerChangeAdapter = new OnViewPagerChangeAdapter(viewPager, this);
        viewPager.addOnPageChangeListener(onViewPagerChangeAdapter);

        final Feature[] features = {
                new Feature(R.string.onboarding_intro_feature_alarm_title,
                            R.string.onboarding_intro_feature_alarm_message),
                new Feature(R.string.onboarding_intro_feature_timeline_title,
                            R.string.onboarding_intro_feature_timeline_message),
                new Feature(R.string.onboarding_intro_feature_sleep_score_title,
                            R.string.onboarding_intro_feature_sleep_score_message),
                new Feature(R.string.onboarding_intro_feature_conditions_title,
                            R.string.onboarding_intro_feature_conditions_message),
        };
        final Adapter adapter = new Adapter(inflater, features,
                                            new SafeOnClickListener(null, this::watchVideo));

        this.pageDots = (PageDots) view.findViewById(R.id.fragment_onboarding_introduction_page_dots);
        Views.runWhenLaidOut(view, () -> {
            // Can be called after the view is destroyed
            // due to the back stack being cleared out.
            if (viewPager != null) {
                viewPager.setAdapter(adapter);
                pageDots.attach(viewPager);
            }
        });

        this.signInButton = (Button) view.findViewById(R.id.fragment_onboarding_introduction_sign_in);
        this.signInLayoutParams = (LinearLayout.LayoutParams) signInButton.getLayoutParams();
        Views.setSafeOnClickListener(signInButton, this::signIn);

        this.buttonDivider = view.findViewById(R.id.fragment_onboarding_introduction_button_divider);

        this.getStartedButton = (Button) view.findViewById(R.id.fragment_onboarding_introduction_get_started);
        Views.setSafeOnClickListener(getStartedButton, this::getStarted);

        final Resources resources = getResources();
        this.introStatusBarColor = resources.getColor(R.color.status_bar_grey);
        this.featureStatusBarColor = resources.getColor(R.color.light_accent_darkened);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("lastSelectedPage", lastSelectedPage);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        onViewPagerChangeAdapter.destroy();
        this.onViewPagerChangeAdapter = null;

        pageDots.detach();
        this.pageDots = null;

        viewPager.clearOnPageChangeListeners();
        viewPager.setAdapter(null);
        this.viewPager = null;

        this.signInButton = null;
        this.signInLayoutParams = null;
        this.buttonDivider = null;
        this.getStartedButton = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.window = null;
    }

    @Override
    public int getStatusBarColor(@NonNull Resources resources) {
        if (lastSelectedPage > INTRO_POSITION) {
            return resources.getColor(R.color.light_accent_darkened);
        } else {
            return resources.getColor(R.color.status_bar_grey);
        }
    }

    @Override
    public void onStatusBarTransitionBegan(@ColorInt int targetColor) {
        this.statusBarChanging = true;
    }

    @Override
    public void onStatusBarTransitionEnded(@ColorInt int finalColor) {
        this.statusBarChanging = false;
    }

    @Override
    public boolean onInterceptBackPressed(@NonNull Runnable defaultBehavior) {
        if (viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(INTRO_POSITION, true);
            return true;
        } else {
            return false;
        }
    }

    //endregion


    //region Actions

    public void getStarted(@NonNull View sender) {
        getFragmentNavigation().flowFinished(this, RESPONSE_GET_STARTED, null);
    }

    public void signIn(@NonNull View sender) {
        getFragmentNavigation().flowFinished(this, RESPONSE_SIGN_IN, null);
    }

    public void watchVideo(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PLAY_VIDEO, null);

        final Bundle arguments = VideoPlayerActivity.getArguments(Uri.parse(UserSupport.VIDEO_URL));
        final Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtras(arguments);
        startActivity(intent);
    }

    //endregion


    //region Pages

    @Override
    public void onPageChangeScrolled(int position, float offset) {
        if (position == INTRO_POSITION) {
            if (!statusBarChanging) {
                final @ColorInt int statusBarColor = Anime.interpolateColors(offset,
                                                                             introStatusBarColor,
                                                                             featureStatusBarColor);
                Windows.setStatusBarColor(window, statusBarColor);
            }

            final float fraction = 1f - offset;
            signInLayoutParams.weight = fraction;
            // To avoid extra layouts immediately after inflate
            if (!signInButton.isInLayout()) {
                signInButton.requestLayout();
            }

            signInButton.setAlpha(fraction);
            buttonDivider.setAlpha(fraction);
        }

        final int alpha = Math.round(255f * offset);
        diagramLayers.getDrawable(DRAWABLE_ON_SCREEN).setAlpha(255 - alpha);
        diagramLayers.getDrawable(DRAWABLE_OFF_SCREEN).setAlpha(alpha);
    }

    @Override
    public void onPageChangeCompleted(int position) {
        final Properties properties = Analytics.createProperties(Analytics.Onboarding.PROP_SCREEN,
                                                                 position + 1);
        Analytics.trackEvent(Analytics.Onboarding.EVENT_INTRO_SWIPED, properties);

        final @ColorInt int statusBarColor;
        final float finalFraction;
        if (position == INTRO_POSITION) {
            statusBarColor = introStatusBarColor;
            finalFraction = 1f;
        } else {
            statusBarColor = featureStatusBarColor;
            finalFraction = 0f;
        }

        if (!statusBarChanging) {
            Windows.setStatusBarColor(window, statusBarColor);
        }

        signInLayoutParams.weight = finalFraction;
        signInButton.requestLayout();

        signInButton.setAlpha(finalFraction);
        buttonDivider.setAlpha(finalFraction);

        final Resources resources = getResources();
        final Drawable onScreen = ResourcesCompat.getDrawable(resources,
                                                              DIAGRAMS[position],
                                                              null);
        diagramLayers.setDrawableByLayerId(DRAWABLE_ON_SCREEN, onScreen);

        final Drawable offScreen = ResourcesCompat.getDrawable(resources,
                                                               DIAGRAMS[position + 1],
                                                               null);
        //noinspection ConstantConditions
        offScreen.setAlpha(0);
        diagramLayers.setDrawableByLayerId(DRAWABLE_OFF_SCREEN, offScreen);

        this.lastSelectedPage = position;
    }

    class Adapter extends ViewPagerAdapter<Adapter.StaticViewHolder> {
        private static final int NON_FEATURE_COUNT = 1;

        private final LayoutInflater inflater;
        private final Feature[] features;
        private final View.OnClickListener onWatchVideoClick;

        Adapter(@NonNull LayoutInflater inflater,
                @NonNull Feature[] features,
                @Nullable View.OnClickListener onWatchVideoClick) {
            this.inflater = inflater;
            this.features = features;
            this.onWatchVideoClick = onWatchVideoClick;
        }

        @Override
        public int getCount() {
            return NON_FEATURE_COUNT + features.length;
        }

        @Override
        public StaticViewHolder createViewHolder(ViewGroup container, int position) {
            if (position == INTRO_POSITION) {
                final View view = inflater.inflate(R.layout.item_onboarding_introduction_first,
                                                   container, false);

                final Button watchVideo =
                        (Button) view.findViewById(R.id.item_onboarding_intro_first_watch_video);
                watchVideo.setOnClickListener(onWatchVideoClick);

                return new StaticViewHolder(view);
            } else {
                final View view = inflater.inflate(R.layout.item_onboarding_introduction_feature,
                                                   container, false);
                return new FeatureViewHolder(view);
            }
        }

        @Override
        public void bindViewHolder(StaticViewHolder holder, int position) {
            holder.bind(position);
        }

        class StaticViewHolder extends ViewPagerAdapter.ViewHolder {
            StaticViewHolder(@NonNull View itemView) {
                super(itemView);

                // Guard against the ViewPager animating after
                // the fragment's view has been destroyed.
                final int pageDotsHeight = pageDots != null ? pageDots.getMeasuredHeight() : 0;
                itemView.setPadding(itemView.getPaddingLeft(),
                                    itemView.getPaddingTop(),
                                    itemView.getPaddingRight(),
                                    itemView.getPaddingBottom() + pageDotsHeight);
            }

            void bind(int position) {
                // Do nothing.
            }
        }

        class FeatureViewHolder extends StaticViewHolder {
            final TextView title;
            final TextView message;

            FeatureViewHolder(@NonNull View itemView) {
                super(itemView);

                final ViewGroup rootView = (ViewGroup) itemView.findViewById(R.id.item_onboarding_introduction_feature_root);
                rootView.setPadding(0, diagramImage.getMeasuredHeight(), 0, 0);

                this.title = (TextView) itemView.findViewById(R.id.item_onboarding_introduction_feature_title);
                this.message = (TextView) itemView.findViewById(R.id.item_onboarding_introduction_feature_message);
            }

            @Override
            void bind(int position) {
                final Feature feature = features[position - NON_FEATURE_COUNT];
                title.setText(feature.title);
                message.setText(feature.message);
            }
        }
    }

    static class Feature {
        final @StringRes int title;
        final @StringRes int message;

        Feature(@StringRes int title,
                @StringRes int message) {
            this.title = title;
            this.message = message;
        }
    }

    //endregion
}
