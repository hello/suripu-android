package is.hello.sense.functional;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ListsTests {
    @Test
    public void testNewArrayList() {
        ArrayList<String> empty = Lists.newArrayList();
        assertNotNull(empty);
        assertEquals(0, empty.size());

        ArrayList<String> single = Lists.newArrayList("one thing");
        assertNotNull(single);
        assertEquals(1, single.size());
        assertEquals("one thing", single.get(0));

        ArrayList<String> multiple = Lists.newArrayList("one thing", "two things", "three things", "more");
        assertNotNull(multiple);
        assertEquals(4, multiple.size());
        assertEquals("one thing", multiple.get(0));
        assertEquals("two things", multiple.get(1));
        assertEquals("three things", multiple.get(2));
        assertEquals("more", multiple.get(3));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Lists.isEmpty(null));
        assertTrue(Lists.isEmpty(Collections.emptyList()));
        assertFalse(Lists.isEmpty(Lists.newArrayList("not", "empty")));
    }

    @Test
    public void testMap() {
        List<String> numberStrings = Lists.newArrayList("1", "2", "3", "4", "5");
        List<Integer> numbers = Lists.map(numberStrings, Integer::parseInt);
        assertEquals(Lists.newArrayList(1, 2, 3, 4, 5), numbers);
    }

    @Test
    public void testTakeEvery() {
        List<Integer> ints = Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        List<Integer> everyThree = Lists.takeEvery(ints, 3);
        assertEquals(Lists.newArrayList(0, 3, 6, 9, 12), everyThree);
    }

    @Test
    public void testSorted() {
        List<Integer> ints = Lists.newArrayList(9, 5, 6, 88);
        List<Integer> sorted = Lists.sorted(ints, Functions::compareInts);
        assertEquals(Lists.newArrayList(5, 6, 9, 88), sorted);
    }

    @Test
    public void testFiltered() {
        List<Integer> ints = Lists.newArrayList(1, 2, 3, 4, 5);
        List<Integer> filteredInts = Lists.filtered(ints, i -> (i % 2) == 0);
        assertEquals(Lists.newArrayList(2, 4), filteredInts);
    }

    @Test
    public void testSumInt() {
        List<Integer> ints = Lists.newArrayList(1, 2, 3, 4, 5);
        assertEquals(15, Lists.sumInt(ints, i -> i));
    }
}
