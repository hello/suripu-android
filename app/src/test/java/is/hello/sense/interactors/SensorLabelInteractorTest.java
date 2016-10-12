package is.hello.sense.interactors;

import org.junit.Test;

import java.util.Calendar;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import is.hello.sense.graph.InjectionTestCase;

public class SensorLabelInteractorTest extends InjectionTestCase {

    @Inject
    SensorLabelInteractor sensorLabelInteractor;

    private Calendar getCalendar() {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, 2016);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        return calendar;

    }

    @Test
    public void getLabelsGeneratesOnce() {
        final SensorLabelInteractor spy = spy(sensorLabelInteractor);

        spy.getWeekLabels();
        spy.getWeekLabels();
        spy.getWeekLabels();
        spy.getWeekLabels();
        spy.getWeekLabels();
        spy.getWeekLabels();
        verify(spy, times(1)).generateWeekLabels(any(Calendar.class));

        spy.getDayLabels();
        spy.getDayLabels();
        spy.getDayLabels();
        spy.getDayLabels();
        spy.getDayLabels();
        spy.getDayLabels();
        verify(spy, times(1)).generateDayLabels(any(Calendar.class));
    }

    @Test
    public void hasCorrectDayLabels() {
        final String[] labels = sensorLabelInteractor.generateDayLabels(getCalendar());

        assertEquals(labels.length, 7);
        assertEquals("1PM", labels[0]);
        assertEquals("5PM", labels[1]);
        assertEquals("8PM", labels[2]);
        assertEquals("12AM", labels[3]);
        assertEquals("3AM", labels[4]);
        assertEquals("7AM", labels[5]);
        assertEquals("10AM", labels[6]);
    }

    @Test
    public void hasCorrectWeekLabels() {
        final String[] labels = sensorLabelInteractor.generateWeekLabels(getCalendar());

        assertEquals(labels.length, 7);
        assertEquals("Sat", labels[0]);
        assertEquals("Sun", labels[1]);
        assertEquals("Mon", labels[2]);
        assertEquals("Tue", labels[3]);
        assertEquals("Wed", labels[4]);
        assertEquals("Thu", labels[5]);
        assertEquals("Fri", labels[6]);
    }

}
