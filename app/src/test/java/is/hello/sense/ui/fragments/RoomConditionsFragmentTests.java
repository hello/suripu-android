package is.hello.sense.ui.fragments;


import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.api.model.v2.sensors.SensorResponse;
import is.hello.sense.api.model.v2.sensors.SensorStatus;
import is.hello.sense.api.model.v2.sensors.SensorsDataResponse;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.view.home.roomconditions.SensorResponseAdapter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.robolectric.util.FragmentTestUtil.startFragment;

public class RoomConditionsFragmentTests extends InjectionTestCase {
    //todo modifying preferencesInteractor has no effect because fragment doesn't use test module provided interactors
    @Inject
    PreferencesInteractor preferencesInteractor;

    private is.hello.sense.mvp.presenters.home.RoomConditionsFragment fragment;

    @Before
    public void setUp() throws Exception {
        /*preferencesInteractor.edit()
                             .clear()
                             .commit();*/
        fragment = new is.hello.sense.mvp.presenters.home.RoomConditionsFragment();
        startFragment(fragment);
    }


    @Test
    public void hasAllViews() {
        assertNotNull(fragment.getView());
        assertNotNull(fragment.getView().findViewById(R.id.fragment_room_conditions_refresh_container));
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
        assertEquals(SensorResponseAdapter.VIEW_AIR_QUALITY, fragment.adapter.getItemViewType(0));
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
        assertEquals(SensorResponseAdapter.VIEW_SENSE_MISSING, fragment.adapter.getItemViewType(0));
    }

    @Test
    public void fragmentShowWelcomeCard() {
        assertEquals(0, fragment.adapter.getItemCount());
        /*preferencesInteractor
                .edit()
                .putInt(PreferencesInteractor.ROOM_CONDITIONS_WELCOME_CARD_TIMES_SHOWN,0)
                .commit();*/
        //fragment.onUpdate();
        //final List<Sensor> sensors = new ArrayList<>(0);
        //final SensorStatus status = SensorStatus.OK;
        //final SensorResponse sensorResponse = new SensorResponse(sensors, status);
        //fragment.bindConditions(sensorResponse);
        fragment.adapter.showWelcomeCard(true);
        assertEquals(1, fragment.adapter.getItemCount());
        assertEquals(SensorResponseAdapter.VIEW_WELCOME_CARD, fragment.adapter.getItemViewType(0));
    }

    @Test
    public void sensorViewHolderBindsCorrectly() {
        /*preferencesInteractor
                .edit()
                .putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                .commit();*/

        final List<Sensor> sensors = Sensor.generateTestCaseList();
        fragment.bindDataResponse(new SensorsDataResponse(), sensors);

        final SensorResponseAdapter.SensorViewHolder viewHolder = fragment.adapter.new SensorViewHolder(
                fragment.getActivity().getLayoutInflater().inflate(R.layout.item_sensor_response, null, false));

        viewHolder.bind(0);
        assertEquals("32 °", viewHolder.value.getText().toString());
        viewHolder.bind(1);
        assertEquals("1 %", viewHolder.value.getText().toString());
        viewHolder.bind(2);
        assertEquals("2.0", viewHolder.value.getText().toString());
        assertEquals("lx", viewHolder.descriptor.getText().toString());
        viewHolder.bind(3);
        assertEquals("4", viewHolder.value.getText().toString());
        assertEquals("dB", viewHolder.descriptor.getText().toString());
    }


}
