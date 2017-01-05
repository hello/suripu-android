package is.hello.sense.api.model.v2.sensors;

import org.junit.Test;

import java.util.ArrayList;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.Constants;

import static junit.framework.Assert.assertEquals;

public class SensorsDataResponseTest extends SenseTestCase {

    @Test
    public void removeLastInvalidSensorDataValues() throws Exception {
        final SensorData invalidData = new SensorData();
        invalidData.put(SensorType.UNKNOWN, new float[] { 1f, 2f, 3f, Constants.NONE });
        final SensorsDataResponse dataResponse = new SensorsDataResponse(invalidData,
                                                                         new ArrayList<>(0));
        dataResponse.removeLastInvalidSensorDataValues();
        final float[] updatedData = dataResponse.getSensorData().get(SensorType.UNKNOWN);
        assertEquals(3f, updatedData[updatedData.length -1]);
        assertEquals(invalidData.get(SensorType.UNKNOWN), updatedData);
    }

    @Test
    public void removeLastInvalidSensorDataValueOnSingleValue() throws Exception {
        final SensorData invalidData = new SensorData();
        invalidData.put(SensorType.UNKNOWN, new float[] { Constants.NONE });
        final SensorsDataResponse dataResponse = new SensorsDataResponse(invalidData,
                                                                         new ArrayList<>(0));
        dataResponse.removeLastInvalidSensorDataValues();
        final float[] updatedData = dataResponse.getSensorData().get(SensorType.UNKNOWN);
        assertEquals(0, updatedData.length);
    }

}