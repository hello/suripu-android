package is.hello.sense.flows.voice.ui.widgets;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.junit.Assert.assertEquals;

public class VolumePickerViewTest extends SenseTestCase {

    @Test
    public void convertFromPercentageValue() throws Exception {
        final VolumePickerView pickerView = new VolumePickerView(getContext());
        pickerView.setMinValue(1);
        pickerView.setMaxValue(11);
        assertEquals(pickerView.convertFromPercentageValue(100), 11);
        assertEquals(pickerView.convertFromPercentageValue(91), 10);
        assertEquals(pickerView.convertFromPercentageValue(82), 9);
        assertEquals(pickerView.convertFromPercentageValue(73), 8);
        assertEquals(pickerView.convertFromPercentageValue(64), 7);
        assertEquals(pickerView.convertFromPercentageValue(55), 6);
        assertEquals(pickerView.convertFromPercentageValue(46), 5);
        assertEquals(pickerView.convertFromPercentageValue(37), 4);
        assertEquals(pickerView.convertFromPercentageValue(28), 3);
        assertEquals(pickerView.convertFromPercentageValue(19), 2);
        assertEquals(pickerView.convertFromPercentageValue(10), 1);
    }



    @Test
    public void convertSelectedValueToPercentageValue() throws Exception {
        final VolumePickerView pickerView = new VolumePickerView(getContext());
        pickerView.setMinValue(1);
        pickerView.setMaxValue(11);
        pickerView.setValue(1, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 10);
        pickerView.setValue(2, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 19);
        pickerView.setValue(3, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 28);
        pickerView.setValue(4, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 37);
        pickerView.setValue(5, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 46);
        pickerView.setValue(6, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 55);
        pickerView.setValue(7, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 64);
        pickerView.setValue(8, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 73);
        pickerView.setValue(9, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 82);
        pickerView.setValue(10, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 91);
        pickerView.setValue(11, false);
        assertEquals(pickerView.convertSelectedValueToPercentageValue(), 100);
    }

}