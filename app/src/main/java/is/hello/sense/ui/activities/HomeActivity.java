package is.hello.sense.ui.activities;

import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.TimelineFragment;

public class HomeActivity extends InjectionActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportFragmentManager().findFragmentByTag("TimelineFragment") == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.activity_home_container, TimelineFragment.newInstance(), "TimelineFragment")
                    .commit();
        }
    }
}
