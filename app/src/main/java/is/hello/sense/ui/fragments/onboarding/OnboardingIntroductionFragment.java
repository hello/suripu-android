package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.ViewPagerAdapter;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.VideoPlayerActivity;
import is.hello.sense.ui.widget.PageDots;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.SafeOnClickListener;

public class OnboardingIntroductionFragment extends SenseFragment {
    private ViewPager viewPager;
    private Button signInButton;
    private Button registerButton;


    //region Lifecycle

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_introduction, container, false);

        this.viewPager = (ViewPager) view.findViewById(R.id.fragment_onboarding_introduction_pager);

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
        Views.setSafeOnClickListener(signInButton, this::showSignIn);

        this.registerButton = (Button) view.findViewById(R.id.fragment_onboarding_introduction_register);
        Views.setSafeOnClickListener(registerButton, this::showRegister);

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

    //endregion


    //region Actions

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    public void showRegister(@NonNull View sender) {
        getOnboardingActivity().showRegistration(false);
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


    static class Adapter extends ViewPagerAdapter<Adapter.StaticViewHolder> {
        private static final int STATIC_COUNT = 1;

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
            return STATIC_COUNT + features.length;
        }

        @Override
        public StaticViewHolder createViewHolder(ViewGroup container, int position) {
            if (position == 0) {
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
                final Feature feature = features[position - STATIC_COUNT];
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
}
