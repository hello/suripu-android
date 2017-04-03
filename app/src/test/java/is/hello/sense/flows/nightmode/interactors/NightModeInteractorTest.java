package is.hello.sense.flows.nightmode.interactors;

import android.location.Location;

import org.joda.time.DateTime;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.flows.nightmode.NightMode;
import is.hello.sense.graph.InjectionTestCase;

import static java.util.TimeZone.getTimeZone;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class NightModeInteractorTest extends InjectionTestCase {
    @Inject
    NightModeInteractor nightModeInteractor;

    @Test
    public void getModeBasedOnLocationAndTime() throws Exception {
        final int mode = nightModeInteractor.getModeBasedOnLocationAndTime();
        assertThat(mode, equalTo(NightMode.OFF));
    }

    @Test
    public void isNightTime() throws Exception {
        final Location locationMock = mock(Location.class);

        //SF lat long
        doReturn(-37.773972).when(locationMock).getLatitude();
        doReturn(-122.431297).when(locationMock).getLongitude();

        final boolean oneAM = nightModeInteractor.isNightTime(locationMock,
                                                               getTimeZone("America/Los_Angeles"),
                                                               DateTime.now().withHourOfDay(0));
        assertThat(oneAM, equalTo(true));

        final boolean tenPM = nightModeInteractor.isNightTime(locationMock,
                                                               getTimeZone("America/Los_Angeles"),
                                                               DateTime.now().withHourOfDay(20));
        assertThat(tenPM, equalTo(true));

        final boolean nineAM = nightModeInteractor.isNightTime(locationMock,
                                                               getTimeZone("America/Los_Angeles"),
                                                               DateTime.now().withHourOfDay(9));
        assertThat(nineAM, equalTo(false));

        final boolean fourPM = nightModeInteractor.isNightTime(locationMock,
                                                               getTimeZone("America/Los_Angeles"),
                                                               DateTime.now().withHourOfDay(16));
        assertThat(fourPM, equalTo(false));
    }

}