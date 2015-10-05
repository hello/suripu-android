package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.handholding.util.WelcomeDialogParser;
import is.hello.sense.util.Analytics;

public class SensorHistoryActivity extends SenseActivity {
    public static final String EXTRA_SENSOR = SensorHistoryActivity.class.getName() + ".EXTRA_SENSOR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_history);

        String sensor = getSensor();
        int titleRes = R.string.app_name;
        switch (sensor) {
            case ApiService.SENSOR_NAME_TEMPERATURE:
                titleRes = R.string.condition_temperature;
                break;

            case ApiService.SENSOR_NAME_PARTICULATES:
                titleRes = R.string.condition_airquality;
                break;

            case ApiService.SENSOR_NAME_HUMIDITY:
                titleRes = R.string.condition_humidity;
                break;

            case ApiService.SENSOR_NAME_LIGHT:
                titleRes = R.string.condition_light;
                break;

            case ApiService.SENSOR_NAME_SOUND:
                titleRes = R.string.condition_sound;

            default:
                break;
        }

        //noinspection ConstantConditions
        getActionBar().setTitle(titleRes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sensor_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sensor_history_help) {
            showWelcomeDialog(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String getSensor() {
        final String sensorName = getIntent().getStringExtra(EXTRA_SENSOR);
        if (TextUtils.isEmpty(sensorName)) {
            Analytics.trackUnexpectedError(new IllegalStateException("EXTRA_SENSOR is absent!"));
            return ApiService.SENSOR_NAME_TEMPERATURE;
        } else {
            return sensorName;
        }
    }

    public boolean showWelcomeDialog(boolean overrideCheck) {
        int welcomeDialogRes;
        switch (getSensor()) {
            case ApiService.SENSOR_NAME_TEMPERATURE: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_temperature;
                break;
            }
            case ApiService.SENSOR_NAME_HUMIDITY: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_humidity;
                break;
            }
            case ApiService.SENSOR_NAME_PARTICULATES: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_airquality;
                break;
            }
            case ApiService.SENSOR_NAME_SOUND: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_noise;
                break;
            }
            case ApiService.SENSOR_NAME_LIGHT: {
                welcomeDialogRes = R.xml.welcome_dialog_sensor_light;
                break;
            }
            default: {
                welcomeDialogRes = WelcomeDialogParser.MISSING_RES;
                break;
            }
        }

        if (overrideCheck) {
            WelcomeDialogFragment.show(this, welcomeDialogRes);
            return true;
        } else {
            return WelcomeDialogFragment.showIfNeeded(this, welcomeDialogRes);
        }
    }
}
