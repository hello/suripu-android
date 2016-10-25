package is.hello.sense.units;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnitOperationsTests {

    @Test
    public void convertFromPercentageValue() throws Exception {
        final int levels = 11;
        assertEquals(UnitOperations.percentageToLevel(100, levels), 11);
        assertEquals(UnitOperations.percentageToLevel(91, levels), 10);
        assertEquals(UnitOperations.percentageToLevel(82, levels), 9);
        assertEquals(UnitOperations.percentageToLevel(73, levels), 8);
        assertEquals(UnitOperations.percentageToLevel(64, levels), 7);
        assertEquals(UnitOperations.percentageToLevel(55, levels), 6);
        assertEquals(UnitOperations.percentageToLevel(46, levels), 5);
        assertEquals(UnitOperations.percentageToLevel(37, levels), 4);
        assertEquals(UnitOperations.percentageToLevel(28, levels), 3);
        assertEquals(UnitOperations.percentageToLevel(19, levels), 2);
        assertEquals(UnitOperations.percentageToLevel(10, levels), 1);
    }

    @Test
    public void levelToPercentage() throws Exception {
        final int levels = 11;
        assertEquals(UnitOperations.levelToPercentage(1, levels), 10);
        assertEquals(UnitOperations.levelToPercentage(2, levels), 19);
        assertEquals(UnitOperations.levelToPercentage(3, levels), 28);
        assertEquals(UnitOperations.levelToPercentage(4, levels), 37);
        assertEquals(UnitOperations.levelToPercentage(5, levels), 46);
        assertEquals(UnitOperations.levelToPercentage(6, levels), 55);
        assertEquals(UnitOperations.levelToPercentage(7, levels), 64);
        assertEquals(UnitOperations.levelToPercentage(8, levels), 73);
        assertEquals(UnitOperations.levelToPercentage(9, levels), 82);

        assertEquals(UnitOperations.levelToPercentage(10, levels), 91);

        assertEquals(UnitOperations.levelToPercentage(11, levels), 100);
    }

}
