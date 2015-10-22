package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import is.hello.go99.Anime;
import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.VideoPlayerActivity;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.ui.widget.util.Windows;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SafeOnClickListener;

public class IntroductionFragment extends SenseFragment implements ViewPager.OnPageChangeListener {
    private static final int INTRO_POSITION = 0;

    private ViewPager viewPager;
    private Button signInButton;
    private LinearLayout.LayoutParams signInLayoutParams;
    private View buttonDivider;
    private Button registerButton;

    private Window window;
    private @ColorInt int introStatusBarColor;
    private @ColorInt int featureStatusBarColor;


    //region Lifecycle

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.window = activity.getWindow();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_introduction, container, false);

        this.viewPager = (ViewPager) view.findViewById(R.id.fragment_onboarding_introduction_pager);
        viewPager.addOnPageChangeListener(this);

        final Feature[] features = {
                new Feature(R.drawable.onboarding_intro_feature_alarm,
                            R.string.onboarding_intro_feature_alarm_title,
                            R.string.onboarding_intro_feature_alarm_message),
                new Feature(R.drawable.onboarding_intro_feature_timeline,
                            R.string.onboarding_intro_feature_timeline_title,
                            R.string.onboarding_intro_feature_timeline_message),
                new Feature(R.drawable.onboarding_intro_feature_sleep_score,
                            R.string.onboarding_intro_feature_sleep_score_title,
                            R.string.onboarding_intro_feature_sleep_score_message),
                new Feature(R.drawable.onboarding_intro_feature_conditions,
                            R.string.onboarding_intro_feature_conditions_title,
                            R.string.onboarding_intro_feature_conditions_message),
        };
        final Adapter adapter = new Adapter(inflater, features,
                                            new SafeOnClickListener(this::watchVideo));
        viewPager.setAdapter(adapter);

        final PageDots pageDots = (PageDots) view.findViewById(R.id.fragment_onboarding_introduction_page_dots);
        pageDots.attach(viewPager);

        this.signInButton = (Button) view.findViewById(R.id.fragment_onboarding_introduction_sign_in);
        this.signInLayoutParams = (LinearLayout.LayoutParams) signInButton.getLayoutParams();
        Views.setSafeOnClickListener(signInButton, this::showSignIn);

        this.buttonDivider = view.findViewById(R.id.fragment_onboarding_introduction_button_divider);

        this.registerButton = (Button) view.findViewById(R.id.fragment_onboarding_introduction_register);
        Views.setSafeOnClickListener(registerButton, this::showRegister);

        final Resources resources = getResources();
        this.introStatusBarColor = resources.getColor(R.color.status_bar_grey);
        this.featureStatusBarColor = resources.getColor(R.color.light_accent_darkened);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        viewPager.clearOnPageChangeListeners();

        this.viewPager = null;
        this.signInButton = null;
        this.registerButton = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.window = null;
    }

    //endregion


    //region Actions

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    public void showRegister(@NonNull View sender) {
        getOnboardingActivity().beginRegistration(false);
    }

    public void showSignIn(@NonNull View sender) {
        getOnboardingActivity().showSignIn();
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
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == INTRO_POSITION) {
            final @ColorInt int statusBarColor = Anime.interpolateColors(positionOffset,
                                                                         introStatusBarColor,
                                                                         featureStatusBarColor);
            Windows.setStatusBarColor(window, statusBarColor);

            final float fraction = 1f - positionOffset;
            signInLayoutParams.weight = fraction;
            signInButton.requestLayout();

            signInButton.setAlpha(fraction);
            buttonDivider.setAlpha(fraction);
        }
    }

    @Override
    public void onPageSelected(int position) {
        final JSONObject properties = Analytics.createProperties(Analytics.Onboarding.PROP_SCREEN,
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

        Windows.setStatusBarColor(window, statusBarColor);

        signInLayoutParams.weight = finalFraction;
        signInButton.requestLayout();

        signInButton.setAlpha(finalFraction);
        buttonDivider.setAlpha(finalFraction);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    static class Adapter extends ViewPagerAdapter<Adapter.StaticViewHolder> {
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
            }

            void bind(int position) {
                // Do nothing.
            }
        }

        class FeatureViewHolder extends StaticViewHolder {
            final ImageView diagram;
            final TextView title;
            final TextView message;

            FeatureViewHolder(@NonNull View itemView) {
                super(itemView);

                this.diagram = (ImageView) itemView.findViewById(R.id.item_onboarding_introduction_feature_diagram);
                this.title = (TextView) itemView.findViewById(R.id.item_onboarding_introduction_feature_title);
                this.message = (TextView) itemView.findViewById(R.id.item_onboarding_introduction_feature_message);
            }

            @Override
            void bind(int position) {
                final Feature feature = features[position - NON_FEATURE_COUNT];
                diagram.setImageResource(feature.diagram);
                title.setText(feature.title);
                message.setText(feature.message);
            }
        }
    }

    static class Feature {
        final @DrawableRes int diagram;
        final @StringRes int title;
        final @StringRes int message;

        Feature(@DrawableRes int diagram,
                @StringRes int title,
                @StringRes int message) {
            this.diagram = diagram;
            this.title = title;
            this.message = message;
        }
    }

    //endregion
}
