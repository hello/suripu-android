package is.hello.sense.ui.activities;

import android.os.Bundle;

import is.hello.sense.R;

public class SensorHistoryActivity extends SenseActivity {
    public static final String EXTRA_SENSOR = SensorHistoryActivity.class.getName() + ".EXTRA_SENSOR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_history);
    }


    public String getSensor() {
        return getIntent().getStringExtra(EXTRA_SENSOR);
    }
}
