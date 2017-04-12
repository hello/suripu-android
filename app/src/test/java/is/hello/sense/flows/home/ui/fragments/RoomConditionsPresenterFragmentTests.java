package is.hello.sense.flows.home.ui.fragments;


import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.FragmentTest;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.api.model.v2.sensors.SensorStatus;
import is.hello.sense.api.model.v2.sensors.SensorsDataResponse;
import is.hello.sense.flows.home.ui.adapters.SensorResponseAdapter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class RoomConditionsPresenterFragmentTests extends FragmentTest<RoomConditionsPresenterFragment> {

    @Test
    public void hasAllViews() {
        assertNotNull(fragment.getView());
        assertNotNull(fragment.getView().findViewById(R.id.fragment_room_conditions_recycler));
        assertNotNull(fragment.getView().findViewById(R.id.fragment_room_conditions_loading));
    }

    @Test
    public void fragmentBindsAdapter() {
        assertEquals(0, fragment.adapter.getItemCount());

        final List<Sensor> sensors = Sensor.generateTestCaseList();
        fragment.bindDataResponse(new SensorsDataResponse(), sensors);
        assertEquals(sensors.get(0), fragment.adapter.getItem(0));
        assertEquals(sensors.get(1), fragment.adapter.getItem(1));
        assertEquals(sensors.get(2), fragment.adapter.getItem(2));
    }

    @Test
    public void adapterCombinesAirQualityCard() {
        assertEquals(0, fragment.adapter.getItemCount());
        final List<Sensor> sensors = new ArrayList<>(3);
        sensors.add(Sensor.newParticulatesTestCase(0f));
        sensors.add(Sensor.newCO2TestCase(0f));
        sensors.add(Sensor.newVOCTestCase(0f));
        fragment.bindDataResponse(new SensorsDataResponse(), sensors);
        assertEquals(1, fragment.adapter.getItemCount());
        assertEquals(SensorResponseAdapter.VIEW_SENSOR_GROUP, fragment.adapter.getItemViewType(0));
    }

    @Test
    public void adapterReturnsSensorTypeCardForSingleAirQualitySensor() {
        assertEquals(0, fragment.adapter.getItemCount());
        final List<Sensor> sensors = new ArrayList<>(1);
        sensors.add(Sensor.newParticulatesTestCase(0f));
        fragment.bindDataResponse(new SensorsDataResponse(), sensors);
        assertEquals(1, fragment.adapter.getItemCount());
        assertEquals(SensorResponseAdapter.VIEW_SENSOR, fragment.adapter.getItemViewType(0));
    }

    @Test
    public void fragmentShowNoSenseCard() {
        assertEquals(0, fragment.adapter.getItemCount());
        final List<Sensor> sensors = new ArrayList<>(0);
        final SensorStatus status = SensorStatus.NO_SENSE;
        final SensorResponse sensorResponse = new SensorResponse(sensors, status);
        fragment.bindConditions(sensorResponse);
        assertEquals(1, fragment.adapter.getItemCount());
        assertEquals(SensorResponseAdapter.VIEW_ID_MESSAGE, fragment.adapter.getItemViewType(0));
    }

    @Test
    public void fragmentShowWelcomeCard() {
        assertEquals(0, fragment.adapter.getItemCount());
        fragment.adapter.showWelcomeCard(true);
        assertEquals(1, fragment.adapter.getItemCount());
        assertEquals(SensorResponseAdapter.VIEW_WELCOME_CARD, fragment.adapter.getItemViewType(0));
    }
}
