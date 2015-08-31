package is.hello.sense.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.RecyclerAdapterTesting;
import is.hello.sense.util.RecyclerAdapterTesting.Observer.Change.Type;

import static is.hello.sense.AssertExtensions.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

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

        observer.assertChangeOccurred(Type.INSERTED,
                0, 2);

        List<String> secondBatch = Lists.newArrayList(
                "goodbye",
                "cruel",
                "world"
        );
        adapter.replaceAll(secondBatch);
        assertTrue(observer.hasObservedChange(Type.CHANGED,
                0, 2));
        assertTrue(observer.hasObservedChange(Type.INSERTED,
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

        observer.assertChangeOccurred(Type.CHANGED,
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

        observer.assertChangeOccurred(Type.CHANGED, 0, 1);
        observer.assertChangeOccurred(Type.REMOVED, 1, 1);
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
        observer.assertChangeOccurred(Type.INSERTED, 0, 2);
    }

    @Test
    public void add() throws Exception {
        adapter.add("hello");
        assertEquals(1, adapter.getItemCount());
        assertEquals("hello", adapter.getItem(0));
        observer.assertChangeOccurred(Type.INSERTED, 0, 1);
    }

    @Test
    public void remove() throws Exception {
        adapter.add("hello");
        assertEquals(1, adapter.getItemCount());
        adapter.remove(0);
        assertEquals(0, adapter.getItemCount());
        observer.assertChangeOccurred(Type.REMOVED, 0, 1);
    }

    @Test
    public void set() throws Exception {
        adapter.add("hello");
        assertEquals(1, adapter.getItemCount());
        assertEquals("hello", adapter.getItem(0));
        adapter.set(0, "world");
        assertEquals(1, adapter.getItemCount());
        assertEquals("world", adapter.getItem(0));
        observer.assertChangeOccurred(Type.CHANGED, 0, 1);
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
        observer.assertChangeOccurred(Type.REMOVED, 0, 2);
    }


    //endregion


    //region Clicks

    @Test
    public void onClick() throws Exception {
        adapter.add("test");

        AtomicInteger clickedPosition = new AtomicInteger(RecyclerView.NO_POSITION);
        adapter.setOnItemClickedListener((position, item) -> clickedPosition.set(position));

        FrameLayout fakeParent = new FrameLayout(getContext());
        ArrayRecyclerAdapter.ViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
                fakeParent, adapter.getItemViewType(0), 0);
        holder.onClick(holder.itemView);

        assertEquals(0, clickedPosition.get());
    }

    @Test
    public void onClickIsStateChangeSafe() throws Exception {
        adapter.add("test");

        adapter.setOnItemClickedListener((position, item) -> {
            fail("onItemClick called with NO_POSITION");
        });

        FrameLayout fakeParent = new FrameLayout(getContext());
        ArrayRecyclerAdapter.ViewHolder holder = RecyclerAdapterTesting.createAndBindView(adapter,
              fakeParent, adapter.getItemViewType(0), 0);
        doReturn(RecyclerView.NO_POSITION).when(holder).getAdapterPosition();
        holder.onClick(holder.itemView);
    }

    //endregion


    static class TestAdapter extends ArrayRecyclerAdapter<String, ArrayRecyclerAdapter.ViewHolder> {
        TestAdapter() {
            super(new ArrayList<>());
        }

        @Override
        public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            ViewHolder viewHolder = new ViewHolder(textView);
            textView.setOnClickListener(viewHolder);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ArrayRecyclerAdapter.ViewHolder holder, int position) {
            TextView text = (TextView) holder.itemView;
            text.setText(getItem(position));
        }
    }
}
