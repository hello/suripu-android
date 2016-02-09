package is.hello.sense.functional;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ListsTests {
    @Test
    public void testNewArrayList() {
        final ArrayList<String> empty = Lists.newArrayList();
        assertThat(empty, is(notNullValue()));
        assertThat(empty.size(), is(equalTo(0)));

        final ArrayList<String> single = Lists.newArrayList("one thing");
        assertThat(single, is(notNullValue()));
        assertThat(single.size(), is(equalTo(1)));
        assertThat(single.get(0), is(equalTo("one thing")));

        final ArrayList<String> multiple = Lists.newArrayList("one thing", "two things", "three things", "more");
        assertThat(multiple, is(notNullValue()));
        assertThat(multiple.size(), is(equalTo(4)));
        assertThat(multiple.get(0), is(equalTo("one thing")));
        assertThat(multiple.get(1), is(equalTo("two things")));
        assertThat(multiple.get(2), is(equalTo("three things")));
        assertThat(multiple.get(3), is(equalTo("more")));
    }

    @Test
    public void testIsEmpty() {
        assertThat(Lists.isEmpty(null), is(true));
        assertThat(Lists.isEmpty(Collections.emptyList()), is(true));
        assertThat(Lists.isEmpty(Lists.newArrayList("not", "empty")), is(false));
    }

    @Test
    public void testMap() {
        final List<String> numberStrings = Lists.newArrayList("1", "2", "3", "4", "5");
        final List<Integer> numbers = Lists.map(numberStrings, Integer::parseInt);
        assertThat(numbers, is(equalTo(Lists.newArrayList(1, 2, 3, 4, 5))));
    }

    @Test
    public void testTakeEvery() {
        final List<Integer> ints = Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        final List<Integer> everyThree = Lists.takeEvery(ints, 3);
        assertThat(everyThree, is(equalTo(Lists.newArrayList(0, 3, 6, 9, 12))));
    }

    @Test
    public void partition() {
        final List<Integer> even = Lists.newArrayList(1, 2, 3,
                                                      1, 2, 3,
                                                      1, 2, 3);
        final List<List<Integer>> evenPartitions = Lists.partition(even, 3);
        assertThat(evenPartitions.size(), is(equalTo(3)));
        for (final List<Integer> partition : evenPartitions) {
            assertThat(partition, is(equalTo(Lists.newArrayList(1, 2, 3))));
        }

        final List<Integer> odd = Lists.newArrayList(1, 2, 3,
                                                     1, 2);
        final List<List<Integer>> oddPartitions = Lists.partition(odd, 3);
        assertThat(oddPartitions.size(), is(equalTo(2)));
        assertThat(oddPartitions.get(0), is(equalTo(Lists.newArrayList(1, 2, 3))));
        assertThat(oddPartitions.get(1), is(equalTo(Lists.newArrayList(1, 2))));
    }

    @Test
    public void testSorted() {
        final List<Integer> ints = Lists.newArrayList(9, 5, 6, 88);
        final List<Integer> sorted = Lists.sorted(ints, Functions::compareInts);
        assertThat(sorted, is(equalTo(Lists.newArrayList(5, 6, 9, 88))));
    }

    @Test
    public void testFiltered() {
        final List<Integer> ints = Lists.newArrayList(1, 2, 3, 4, 5);
        final List<Integer> filteredInts = Lists.filtered(ints, i -> (i % 2) == 0);
        assertThat(filteredInts, is(equalTo(Lists.newArrayList(2, 4))));
    }

    @Test
    public void testSumInt() {
        final List<Integer> ints = Lists.newArrayList(1, 2, 3, 4, 5);
        assertThat(Lists.sumInt(ints, i -> i), is(equalTo(15)));
    }
}
