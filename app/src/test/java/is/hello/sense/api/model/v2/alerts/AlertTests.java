package is.hello.sense.api.model.v2.alerts;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AlertTests extends SenseTestCase {

    @Test
    public void isValid() {
        final Alert emptyAlert = Alert.NewEmptyInstance();
        assertThat(emptyAlert.isValid(), is(false));

        final Alert alertWithBody = new Alert(null, "test", Category.UNKNOWN);
        assertThat(alertWithBody.isValid(), is(true));

        final Alert alertWithNoBody = new Alert(null, "", Category.EXPANSION_UNREACHABLE);
        assertThat(alertWithNoBody.isValid(), is(false));
    }

}
