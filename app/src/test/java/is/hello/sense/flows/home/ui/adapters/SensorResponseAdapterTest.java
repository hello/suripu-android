package is.hello.sense.flows.home.ui.adapters;

import android.view.LayoutInflater;
import android.widget.FrameLayout;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.RecyclerAdapterTesting;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

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

        final FrameLayout fakeParent = new FrameLayout(getContext());

        SensorResponseAdapter.SensorViewHolder viewHolder = RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);

        assertEquals("0°", viewHolder.value.getText().toString());
        viewHolder = RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 1);
        assertEquals("1%", viewHolder.value.getText().toString());
        viewHolder = RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 2);
        assertEquals("2.0", viewHolder.value.getText().toString());
        assertEquals("lx", viewHolder.descriptor.getText().toString());

        final SensorResponseAdapter.SensorGroupViewHolder groupViewHolder = RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 3);

        assertEquals("Air Quality", groupViewHolder.sensorGroupBinding.itemSensorGroupTitle.getText().toString());
        assertEquals("message", groupViewHolder.sensorGroupBinding.itemSensorGroupBody.getText().toString());
        assertThat(groupViewHolder.sensorGroupBinding.itemSensorGroupContent.getChildCount(), Matchers.greaterThan(2));
    }

}