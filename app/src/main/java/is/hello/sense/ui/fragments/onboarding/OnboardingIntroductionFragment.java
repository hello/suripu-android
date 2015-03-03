package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animation;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.VideoPlayerActivity;
import is.hello.sense.ui.widget.PanImageView;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingIntroductionFragment extends SenseFragment implements FragmentNavigation.BackInterceptingFragment {

    private PanImageView sceneBackground;
    private TextView titleText;
    private TextView infoText;
    private ImageView play;
    private ViewGroup accountActions;
    private ViewGroup getStartedActions;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_introduction, container, false);

        this.sceneBackground = (PanImageView) view.findViewById(R.id.fragment_onboarding_introduction_scene);

        this.titleText = (TextView) view.findViewById(R.id.fragment_onboarding_intro_title);
        this.infoText = (TextView) view.findViewById(R.id.fragment_onboarding_intro_info);

        this.getStartedActions = (ViewGroup) view.findViewById(R.id.fragment_onboarding_intro_account_get_started);

        Button getStarted = (Button) getStartedActions.findViewById(R.id.fragment_onboarding_intro_get_started);
        Views.setSafeOnClickListener(getStarted, this::getStarted);

        Button buySense = (Button) getStartedActions.findViewById(R.id.fragment_onboarding_intro_buy_sense);
        Views.setSafeOnClickListener(buySense, this::buySense);


        this.play = (ImageView) view.findViewById(R.id.fragment_onboarding_intro_play);
        Views.setSafeOnClickListener(play, this::playIntroVideo);


        this.accountActions = (ViewGroup) view.findViewById(R.id.fragment_onboarding_intro_account_actions);

        Button register = (Button) accountActions.findViewById(R.id.fragment_onboarding_intro_register);
        Views.setSafeOnClickListener(register, this::showRegister);

        Button signIn = (Button) accountActions.findViewById(R.id.fragment_onboarding_intro_sign_in);
        Views.setSafeOnClickListener(signIn, this::showSignIn);

        Button cancel = (Button) accountActions.findViewById(R.id.fragment_onboarding_intro_cancel);
        Views.setSafeOnClickListener(cancel, this::cancel);


        return view;
    }

    @Override
    public boolean onInterceptBack(@NonNull Runnable back) {
        if (infoText.getAlpha() != 1f) {
            cancel(accountActions);
            return true;
        }

        return false;
    }

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void getStarted(@NonNull View sender) {
        animate(getStartedActions)
                .fadeOut(View.INVISIBLE)
                .start();

        animate(accountActions)
                .fadeIn()
                .start();

        animate(infoText)
                .fadeOut(View.INVISIBLE)
                .start();

        animate(play)
                .slideXAndFade(0f, -(play.getMeasuredWidth() / 2), 1f, 0f)
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        play.setVisibility(View.INVISIBLE);
                    }
                })
                .start();

        animate(titleText)
                .setDuration(Animation.DURATION_NORMAL / 2)
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(finished -> {
                    titleText.setText(R.string.welcome);

                    Analytics.trackEvent(Analytics.Onboarding.EVENT_START, null);
                })
                .andThen()
                .fadeIn()
                .start();

        sceneBackground.animateToPanAmount(1f, Animation.DURATION_NORMAL, null);
    }

    public void playIntroVideo(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_PLAY_VIDEO, null);

        Bundle arguments = VideoPlayerActivity.getArguments(Uri.parse("http://player.vimeo.com/external/101139949.hd.mp4?s=28ac378e29847b77e9fb7431f05d2772"));
        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtras(arguments);
        startActivity(intent);
    }

    public void buySense(@NonNull View sender) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_NO_SENSE, null);
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.ORDER_URL));
    }

    public void showRegister(@NonNull View sender) {
        getOnboardingActivity().showRegistration();
    }

    public void showSignIn(@NonNull View sender) {
        getOnboardingActivity().showSignIn();
    }

    public void cancel(@NonNull View sender) {
        animate(getStartedActions)
                .fadeIn()
                .start();

        animate(accountActions)
                .fadeOut(View.INVISIBLE)
                .start();

        animate(infoText)
                .fadeIn()
                .start();

        animate(play)
                .slideXAndFade(0f, play.getMeasuredWidth() / 2, 0f, 1f)
                .start();

        animate(titleText)
                .setDuration(Animation.DURATION_NORMAL /2)
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(finished -> {
                    titleText.setText(R.string.title_introduction);
                })
                .andThen()
                .fadeIn()
                .start();

        sceneBackground.animateToPanAmount(0f, Animation.DURATION_NORMAL, null);
    }
}
