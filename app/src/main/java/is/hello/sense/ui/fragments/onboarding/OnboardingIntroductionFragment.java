package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animation;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class OnboardingIntroductionFragment extends Fragment {

    private TextView titleText;
    private TextView infoText;
    private ImageView play;
    private ViewGroup accountActions;
    private ViewGroup getStartedActions;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_introduction, container, false);

        this.titleText = (TextView) view.findViewById(R.id.fragment_onboarding_intro_title);
        this.infoText = (TextView) view.findViewById(R.id.fragment_onboarding_intro_info);

        this.getStartedActions = (ViewGroup) view.findViewById(R.id.fragment_onboarding_intro_account_get_started);

        Button getStarted = (Button) getStartedActions.findViewById(R.id.fragment_onboarding_intro_get_started);
        getStarted.setOnClickListener(this::getStarted);

        Button buySense = (Button) getStartedActions.findViewById(R.id.fragment_onboarding_intro_buy_sense);
        buySense.setOnClickListener(this::buySense);


        this.play = (ImageView) view.findViewById(R.id.fragment_onboarding_intro_play);


        this.accountActions = (ViewGroup) view.findViewById(R.id.fragment_onboarding_intro_account_actions);

        Button register = (Button) accountActions.findViewById(R.id.fragment_onboarding_intro_register);
        register.setOnClickListener(this::showRegister);

        Button signIn = (Button) accountActions.findViewById(R.id.fragment_onboarding_intro_sign_in);
        signIn.setOnClickListener(this::showSignIn);

        Button cancel = (Button) accountActions.findViewById(R.id.fragment_onboarding_intro_cancel);
        cancel.setOnClickListener(this::cancel);


        return view;
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
                .fadeOut(View.INVISIBLE)
                .scale(0f)
                .start();

        animate(titleText)
                .setDuration(Animation.DURATION_DEFAULT / 2)
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(finished -> {
                    titleText.setText(R.string.welcome);
                    titleText.setGravity(Gravity.CENTER);
                })
                .andThen()
                .fadeIn()
                .start();
    }

    public void buySense(@NonNull View sender) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://hello.is")));
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
                .fadeIn()
                .scale(1f)
                .start();

        animate(titleText)
                .setDuration(Animation.DURATION_DEFAULT/2)
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(finished -> {
                    titleText.setText(R.string.title_introduction);
                    titleText.setGravity(Gravity.LEFT);
                })
                .andThen()
                .fadeIn()
                .start();
    }
}
