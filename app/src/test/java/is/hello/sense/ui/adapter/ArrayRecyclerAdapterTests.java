package is.hello.sense.ui.adapter;

import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.RecyclerAdapterTesting;

import static is.hello.sense.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArrayRecyclerAdapterTests extends SenseTestCase {
    private final TestAdapter adapter = new TestAdapter();
    private final RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();


    //region Lifecycle

    public ArrayRecyclerAdapterTests() {
        adapter.registerAdapterDataObserver(observer);
    }

    @After
    public void tearDown() {
        observer.reset();
        adapter.clear();
        adapter.setOnItemClickedListener(null);
    }

    //endregion

    
    //region Replace All

    @Test
    public void replaceAllInsertion() throws Exception {
        RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        List<String> firstBatch = Lists.newArrayList(
                "hello",
                "world"
        );
        adapter.replaceAll(firstBatch);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                0, 2);

        List<String> secondBatch = Lists.newArrayList(
                "goodbye",
                "cruel",
                "world"
        );
        adapter.replaceAll(secondBatch);
        assertTrue(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                0, 2));
        assertTrue(observer.hasObservedChange(RecyclerAdapterTesting.Observer.Change.Type.INSERTED,
                2, 1));
    }

    @Test
    public void replaceAllChange() throws Exception {
        List<String> strings = Lists.newArrayList(
                "hello",
                "world"
        );
        adapter.replaceAll(strings);

        RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        adapter.replaceAll(strings);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                0, 2);
    }

    @Test
    public void replaceAllRemove() throws Exception {
        List<String> firstBatch = Lists.newArrayList(
                "hello",
                "world"
        );
        adapter.replaceAll(firstBatch);

        RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();
        adapter.registerAdapterDataObserver(observer);

        List<String> secondBatch = Lists.newArrayList(
                "goodbye"
        );
        adapter.replaceAll(secondBatch);

        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED,
                0, 1);
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.REMOVED,
                1, 1);
    }
    
    //endregion
    

    //region Operations

    @Test
    public void getItemCount() throws Exception {
        adapter.add("one");
        adapter.add("two");
        adapter.add("three");
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void isEmpty() throws Exception {
        assertTrue(adapter.isEmpty());
        adapter.add("whatever");
        assertFalse(adapter.isEmpty());
    }

    @Test
    public void getItem() throws Exception {
        adapter.add("whatever");
        assertEquals("whatever", adapter.getItem(0));
        assertThrows(() -> adapter.getItem(1));
    }

    @Test
    public void addAll() throws Exception {
        adapter.addAll(Lists.newArrayList("hello", "world"));
        assertEquals(2, adapter.getItemCount());
        assertEquals("hello", adapter.getItem(0));
        assertEquals("world", adapter.getItem(1));
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.INSERTED, 0, 2);
    }

    @Test
    public void add() throws Exception {
        adapter.add("hello");
        assertEquals(1, adapter.getItemCount());
        assertEquals("hello", adapter.getItem(0));
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.INSERTED, 0, 1);
    }

    @Test
    public void remove() throws Exception {
        adapter.add("hello");
        assertEquals(1, adapter.getItemCount());
        adapter.remove(0);
        assertEquals(0, adapter.getItemCount());
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.REMOVED, 0, 1);
    }

    @Test
    public void set() throws Exception {
        adapter.add("hello");
        assertEquals(1, adapter.getItemCount());
        assertEquals("hello", adapter.getItem(0));
        adapter.set(0, "world");
        assertEquals(1, adapter.getItemCount());
        assertEquals("world", adapter.getItem(0));
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.CHANGED, 0, 1);
    }

    @Test
    public void indexOf() throws Exception {
        adapter.add("hello");
        adapter.add("world");
        assertEquals(0, adapter.indexOf("hello"));
        assertEquals(1, adapter.indexOf("world"));
    }

    @Test
    public void clear() throws Exception {
        adapter.addAll(Lists.newArrayList("hello", "world"));
        assertEquals(2, adapter.getItemCount());
        adapter.clear();
        assertEquals(0, adapter.getItemCount());
        observer.assertChangeOccurred(RecyclerAdapterTesting.Observer.Change.Type.REMOVED, 0, 2);
    }


    //endregion


    static class TestAdapter extends ArrayRecyclerAdapter<String, ArrayRecyclerAdapter.ViewHolder> {
        TestAdapter() {
            super(new ArrayList<>());
        }

        @Override
        public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(ArrayRecyclerAdapter.ViewHolder holder, int position) {
            TextView text = (TextView) holder.itemView;
            text.setText(getItem(position));
        }
    }
}
