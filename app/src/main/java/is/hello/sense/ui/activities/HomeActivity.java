package is.hello.sense.ui.activities;

import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;

public class HomeActivity extends InjectionActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }
}
