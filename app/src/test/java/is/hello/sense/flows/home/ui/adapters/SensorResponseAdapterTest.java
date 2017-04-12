package is.hello.sense.flows.home.ui.adapters;

import android.view.LayoutInflater;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.units.UnitFormatter;

import static junit.framework.Assert.assertEquals;

public class SensorResponseAdapterTest extends InjectionTestCase {

    @Inject
    UnitFormatter unitFormatter;

    @Inject
    PreferencesInteractor preferencesInteractor;

    private SensorResponseAdapter adapter;

    private boolean initialCelsius;

    @Before
    public void setUp() throws Exception {
        initialCelsius = preferencesInteractor.getBoolean(PreferencesInteractor.USE_CELSIUS, true);

        preferencesInteractor
                .edit()
                .putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                .commit();

        adapter = new SensorResponseAdapter(LayoutInflater.from(getContext()),
                                            unitFormatter);
    }

    @After
    public void tearDown() throws Exception {
        if (adapter != null) {
            adapter.release();
            adapter = null;
        }

        preferencesInteractor
                .edit()
                .putBoolean(PreferencesInteractor.USE_CELSIUS, initialCelsius)
                .commit();
    }

    @Test
    public void sensorViewHolderBindsCorrectly() {
        final List<Sensor> sensors = Sensor.generateTestCaseList();
        adapter.replaceAll(sensors);

        final SensorResponseAdapter.SensorViewHolder viewHolder = adapter.new SensorViewHolder(
                LayoutInflater.from(getContext()).inflate(R.layout.item_sensor_response,
                                                          Mockito.any(),
                                                          false));

        viewHolder.bind(0);
        assertEquals("0°", viewHolder.value.getText().toString());
        viewHolder.bind(1);
        assertEquals("1%", viewHolder.value.getText().toString());
        viewHolder.bind(2);
        assertEquals("2.0", viewHolder.value.getText().toString());
        assertEquals("lx", viewHolder.descriptor.getText().toString());
        viewHolder.bind(3);
        assertEquals("3", viewHolder.value.getText().toString());
        assertEquals("µg/m³", viewHolder.descriptor.getText().toString());
    }

}