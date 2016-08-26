package is.hello.sense.ui.fragments;


import org.junit.Before;
import org.junit.Test;

import is.hello.sense.R;
import is.hello.sense.api.model.RoomConditions;
import is.hello.sense.api.model.RoomSensorHistory;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.interactors.RoomConditionsInteractor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.robolectric.util.FragmentTestUtil.startFragment;

public class RoomConditionsFragmentTests extends SenseTestCase {

    RoomConditionsFragment fragment;

    @Before
    public void setUp() throws Exception {
        fragment = new RoomConditionsFragment();
        startFragment(fragment);
    }


    @Test
    public void hasAllViews() {
        assertNotNull(fragment.getView());
        assertNotNull(fragment.getView().findViewById(R.id.fragment_room_conditions_refresh_container));
        assertNotNull(fragment.getView().findViewById(R.id.fragment_room_conditions_recycler));
    }

    @Test
    public void fragmentBindsAdapter() {
        assertEquals(fragment.adapter.getItemCount(), 0);

        RoomConditions conditions = RoomConditions.generateTestExample();
        RoomSensorHistory sensorHistory = new RoomSensorHistory();
        RoomConditionsInteractor.Result result = new RoomConditionsInteractor.Result(conditions, sensorHistory);

        fragment.bindConditions(result);

        assertEquals(fragment.adapter.getItemCount(), 4);

    }

    @Test
    public void sensorViewHolderBindsCorrectly() {
        RoomConditions conditions = RoomConditions.generateTestExample();
        RoomSensorHistory sensorHistory = new RoomSensorHistory();
        RoomConditionsInteractor.Result result = new RoomConditionsInteractor.Result(conditions, sensorHistory);

        fragment.bindConditions(result);

        RoomConditionsFragment.Adapter.SensorViewHolder viewHolder = fragment.adapter.new SensorViewHolder(
                fragment.getActivity().getLayoutInflater().inflate(R.layout.item_room_sensor_condition, null, false));

        viewHolder.bind(0);
        assertEquals(viewHolder.reading.getText().toString(), "212 °");
        viewHolder.bind(1);
        assertEquals(viewHolder.reading.getText().toString(), "60 %");
        viewHolder.bind(2);
        assertEquals(viewHolder.reading.getText().toString(), "-- lux");
        viewHolder.bind(3);
        assertEquals(viewHolder.reading.getText().toString(), "--");


    }


}
