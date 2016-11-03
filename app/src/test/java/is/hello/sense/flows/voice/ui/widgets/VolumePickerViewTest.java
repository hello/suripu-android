package is.hello.sense.flows.voice.ui.widgets;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class VolumePickerViewTest extends SenseTestCase {

    @Test
    public void notifyListenerOnSetValue() throws Exception {
        final VolumePickerView pickerView = new VolumePickerView(getContext());
        final VolumePickerView.OnValueChangedListener listener = mock(VolumePickerView.OnValueChangedListener.class);
        final int value = 1;

        pickerView.setOnValueChangedListener(listener);

        pickerView.setValue(value, true);
        pickerView.setValue(value, false);
        pickerView.setValue(value, false);

        verify(listener , times(1)).onValueChanged(value);
    }

}