package is.hello.sense.flows.nightmode.ui.activities;

import is.hello.sense.R;
import is.hello.sense.flows.nightmode.ui.fragments.NightModeFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class NightModeActivity extends FragmentNavigationActivity {

    @Override
    protected void onCreateAction() {
        navigationDelegate.pushFragment(new NightModeFragment(),
                                        null,
                                        false);
    }

    /**
     * Using {@link android.view.Window#setWindowAnimations(int)} did not work for versions newer
     * than {@link android.os.Build.VERSION_CODES#M} so faking recreate to achieve fade animation.
     * Not setting through theme because don't want to fade in or out if Night Mode did not change.
     */
    @Override
    public void recreate() {
        finish();
        overridePendingTransition(R.anim.anime_fade_in,
                                  R.anim.anime_fade_out);
        startActivity(getIntent());
        overridePendingTransition(R.anim.anime_fade_in,
                                  R.anim.anime_fade_out);
    }
}
