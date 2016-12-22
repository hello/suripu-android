package is.hello.sense.units;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnitOperationsTests {

    @Test
    public void convertFromPercentageValue() throws Exception {
        final int levels = 11;
        assertEquals(11, UnitOperations.percentageToLevel(100, levels));
        assertEquals(11, UnitOperations.percentageToLevel(91, levels));
        assertEquals(10, UnitOperations.percentageToLevel(82, levels));
        assertEquals(9, UnitOperations.percentageToLevel(80, levels));
        assertEquals(9, UnitOperations.percentageToLevel(73, levels));
        assertEquals(8, UnitOperations.percentageToLevel(64, levels));
        assertEquals(7, UnitOperations.percentageToLevel(55, levels));
        assertEquals(6, UnitOperations.percentageToLevel(46, levels));
        assertEquals(5, UnitOperations.percentageToLevel(37, levels));
        assertEquals(4, UnitOperations.percentageToLevel(28, levels));
        assertEquals(3, UnitOperations.percentageToLevel(19, levels));
        assertEquals(2, UnitOperations.percentageToLevel(10, levels));
    }

    @Test
    public void levelToPercentage() throws Exception {
        final int levels = 11;
        assertEquals(9, UnitOperations.levelToPercentage(1, levels));
        assertEquals(18, UnitOperations.levelToPercentage(2, levels));
        assertEquals(27, UnitOperations.levelToPercentage(3, levels));
        assertEquals(36, UnitOperations.levelToPercentage(4, levels));
        assertEquals(45, UnitOperations.levelToPercentage(5, levels));
        assertEquals(55, UnitOperations.levelToPercentage(6, levels));
        assertEquals(64, UnitOperations.levelToPercentage(7, levels));
        assertEquals(73, UnitOperations.levelToPercentage(8, levels));
        assertEquals(82, UnitOperations.levelToPercentage(9, levels));

        assertEquals(91, UnitOperations.levelToPercentage(10, levels));

        assertEquals(100, UnitOperations.levelToPercentage(11, levels));
    }

}
