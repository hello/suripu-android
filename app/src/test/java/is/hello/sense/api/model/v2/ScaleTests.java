package is.hello.sense.api.model.v2;

import android.content.res.Resources;

import org.junit.Test;

import is.hello.sense.api.model.Condition;
import is.hello.sense.graph.SenseTestCase;

import static junit.framework.Assert.assertEquals;

public class ScaleTests extends SenseTestCase{

    @Test
    public void formatValuesToStrings() {
        assertEquals("1", Scale.format(1.9f));
        assertEquals("1", Scale.format(1.99999f));
        assertEquals("-2", Scale.format(-1.9f));
        assertEquals("-2", Scale.format(-1.9999f));
    }

    @Test
    public void displayMinMaxStringsProperly(){
        final Resources resources = getResources();

        assertEquals("", new Scale("empty", null, null, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
        assertEquals("0 to 1", new Scale("normal", 0.0f, 1.0f, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
        assertEquals("0 to 1", new Scale("empty min", null, 1.0f, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
        assertEquals("", new Scale("zero min empty max", 0.0f, null, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
        assertEquals("1+", new Scale("positive min empty max", 1.0f, null, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
        assertEquals("", new Scale("negative min empty max", -999.9f, null, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
        assertEquals("1 to -1", new Scale("nonzero min negative max", 1.0f, -1.0f, Condition.UNKNOWN)
                .getScaleViewValueText(resources));

        assertEquals("-1<", new Scale("empty min negative max", null, -1.0f, Condition.UNKNOWN)
                .getScaleViewValueText(resources));
    }
}
