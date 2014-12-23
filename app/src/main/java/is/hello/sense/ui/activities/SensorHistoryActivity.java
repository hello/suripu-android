package is.hello.sense.ui.activities;

import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.api.model.SensorHistory;

public class SensorHistoryActivity extends SenseActivity {
    public static final String EXTRA_SENSOR = SensorHistoryActivity.class.getName() + ".EXTRA_SENSOR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_history);

        String sensor = getSensor();
        int titleRes = R.string.app_name;
        switch (sensor) {
            case SensorHistory.SENSOR_NAME_TEMPERATURE:
                titleRes = R.string.condition_temperature;
                break;

            case SensorHistory.SENSOR_NAME_PARTICULATES:
                titleRes = R.string.condition_particulates;
                break;

            case SensorHistory.SENSOR_NAME_HUMIDITY:
                titleRes = R.string.condition_humidity;
                break;

            default:
                break;
        }

        //noinspection ConstantConditions
        getActionBar().setTitle(titleRes);
    }


    public String getSensor() {
        return getIntent().getStringExtra(EXTRA_SENSOR);
    }
}
