package is.hello.sense.flows.home.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import is.hello.sense.R;

/**
 * Will eventually replace {@link HomeActivity}
 */

public class NewHomeActivity extends is.hello.sense.ui.common.ScopedInjectionActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_home);
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.activity_new_home_bottom_navigation_view);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(null);
            bottomNavigationView = null;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_timeline:
                break;
            case R.id.action_trends:
                break;
            case R.id.action_insights:
                break;
            case R.id.action_sounds:
                break;
            case R.id.action_conditions:
                break;
        }
        return false;
    }
}
