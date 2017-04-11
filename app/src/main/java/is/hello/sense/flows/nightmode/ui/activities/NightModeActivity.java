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

    @Override
    public void recreate() {
        getWindow().setWindowAnimations(R.style.WindowAnimations_Fade);
        super.recreate();

    }
}
