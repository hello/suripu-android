package is.hello.sense.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.OnboardingRegisterFragment;
import is.hello.sense.ui.fragments.OnboardingSignInFragment;

public class OnboardingActivity extends InjectionActivity {
    private static final String FRAGMENT_TAG = "OnboardingFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        if (savedInstanceState == null)
            showFragment(new OnboardingRegisterFragment());
    }


    private void showFragment(@NonNull Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG) == null) {
            transaction.add(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
        } else {
            transaction.replace(R.id.activity_onboarding_container, fragment, FRAGMENT_TAG);
        }
        transaction.commit();
    }


    public void showHomeActivity() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
