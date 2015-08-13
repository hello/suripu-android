package is.hello.sense.api.model;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.functional.Lists;

import static is.hello.sense.functional.Lists.map;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class RoomConditionsTests {
    @Test
    public void isEmpty() throws Exception {
        RoomConditions conditions = new RoomConditions();
        assertThat(conditions.isEmpty(), is(true));

        conditions.temperature = new SensorState();
        conditions.humidity = new SensorState();
        conditions.light = new SensorState();
        conditions.particulates = new SensorState();
        conditions.sound = new SensorState();
        assertThat(conditions.isEmpty(), is(true));

        conditions.temperature = new SensorState(50, "", Condition.IDEAL, "?", DateTime.now());
        conditions.humidity = new SensorState(50, "", Condition.IDEAL, "?", DateTime.now());
        conditions.light = new SensorState(50, "", Condition.IDEAL, "?", DateTime.now());
        conditions.particulates = new SensorState(50, "", Condition.IDEAL, "?", DateTime.now());
        conditions.sound = new SensorState(50, "", Condition.IDEAL, "?", DateTime.now());
        assertThat(conditions.isEmpty(), is(false));
    }

    @Test
    public void accessorsWhenEmpty() throws Exception {
        RoomConditions empty = new RoomConditions();
        empty.getSound();
        empty.getLight();
        empty.getTemperature();
        empty.getHumidity();
        empty.getParticulates();
    }

    @Test
    public void sensorNamesSet() throws Exception {
        RoomConditions conditions = new RoomConditions();
        conditions.temperature = new SensorState();
        conditions.humidity = new SensorState();
        conditions.light = new SensorState();
        conditions.particulates = new SensorState();
        conditions.sound = new SensorState();

        assertThat(conditions.getTemperature().getName(),
                   is(notNullValue()));
        assertThat(conditions.getHumidity().getName(),
                   is(notNullValue()));
        assertThat(conditions.getLight().getName(),
                   is(notNullValue()));
        assertThat(conditions.getParticulates().getName(),
                   is(notNullValue()));
        assertThat(conditions.getSound().getName(),
                   is(notNullValue()));
    }

    @Test
    public void toListOrder() throws Exception {
        RoomConditions conditions = new RoomConditions();
        conditions.temperature = new SensorState();
        conditions.humidity = new SensorState();
        conditions.light = new SensorState();
        conditions.particulates = new SensorState();
        conditions.sound = new SensorState();

        List<SensorState> sensors = conditions.toList();
        assertThat(sensors, not(hasItem(nullValue())));

        List<String> names = map(sensors, SensorState::getName);
        List<String> expected = Lists.newArrayList(ApiService.SENSOR_NAME_TEMPERATURE,
                                                   ApiService.SENSOR_NAME_HUMIDITY,
                                                   ApiService.SENSOR_NAME_LIGHT,
                                                   ApiService.SENSOR_NAME_SOUND);
        assertThat(names, is(equalTo(expected)));
    }
}
