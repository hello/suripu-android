package is.hello.sense.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;

import is.hello.sense.functional.Lists;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.RecyclerAdapterTesting;
import is.hello.sense.util.RecyclerAdapterTesting.Observer.Change.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HeaderRecyclerAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final TestAdapter adapter = new TestAdapter();
    private final HeaderRecyclerAdapter wrapper = new HeaderRecyclerAdapter(adapter);
    private final RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();

    public HeaderRecyclerAdapterTests() {
        wrapper.registerAdapterDataObserver(observer);
    }

    @After
    public void tearDown() {
        wrapper.headers.clear();
        wrapper.footers.clear();

        adapter.setOnItemClickedListener(null);
        adapter.clear();

        observer.reset();
    }

    private void addHeaders(int count) {
        for (int i = 0; i < count; i++) {
            wrapper.addHeader(new View(getContext()));
        }
    }

    private void addFooters(int count) {
        for (int i = 0; i < count; i++) {
            wrapper.addFooter(new View(getContext()));
        }
    }

    private void addContent() {
        adapter.addAll(Lists.newArrayList("hello", "fabulous", "world"));
    }


    @Test
    public void basicMethods() throws Exception {
        addHeaders(2);
        addFooters(2);
        addContent();

        assertEquals(7, wrapper.getItemCount());

        assertEquals(-2, wrapper.getItemId(0));
        assertEquals(-1, wrapper.getItemId(1));
        assertEquals(0, wrapper.getItemId(2));
        assertEquals(0, wrapper.getItemId(3));
        assertEquals(0, wrapper.getItemId(4));
        assertEquals(Integer.MAX_VALUE, wrapper.getItemId(5));
        assertEquals(Integer.MAX_VALUE - 1, wrapper.getItemId(6));

        assertEquals(HeaderRecyclerAdapter.VIEW_ID_HEADER_FOOTER, wrapper.getItemViewType(0));
        assertEquals(HeaderRecyclerAdapter.VIEW_ID_HEADER_FOOTER, wrapper.getItemViewType(1));
        assertEquals(0, wrapper.getItemId(2));
        assertEquals(0, wrapper.getItemId(3));
        assertEquals(0, wrapper.getItemId(4));
        assertEquals(HeaderRecyclerAdapter.VIEW_ID_HEADER_FOOTER, wrapper.getItemViewType(5));
        assertEquals(HeaderRecyclerAdapter.VIEW_ID_HEADER_FOOTER, wrapper.getItemViewType(6));
    }

    @Test
    public void headers() throws Exception {
        addContent();
        assertEquals(3, wrapper.getItemCount());

        addHeaders(1);
        assertEquals(4, wrapper.getItemCount());
        assertEquals(HeaderRecyclerAdapter.VIEW_ID_HEADER_FOOTER, wrapper.getItemViewType(0));
        assertEquals(0, wrapper.getItemId(1));
        observer.assertChangeOccurred(Type.INSERTED, 0, 1);

        RecyclerView.ViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(wrapper,
                fakeParent, wrapper.getItemViewType(0), 0);
        assertTrue(holder1 instanceof HeaderRecyclerAdapter.HeaderFooterViewHolder);

        RecyclerView.ViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(wrapper,
                fakeParent, wrapper.getItemViewType(1), 1);
        assertTrue(holder2 instanceof ArrayRecyclerAdapter.ViewHolder);
    }

    @Test
    public void footers() throws Exception {
        addContent();
        assertEquals(3, wrapper.getItemCount());

        addFooters(1);
        assertEquals(4, wrapper.getItemCount());
        assertEquals(0, wrapper.getItemId(0));
        assertEquals(HeaderRecyclerAdapter.VIEW_ID_HEADER_FOOTER, wrapper.getItemViewType(3));
        observer.assertChangeOccurred(Type.INSERTED, 3, 1);

        RecyclerView.ViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(wrapper,
                fakeParent, wrapper.getItemViewType(3), 3);
        assertTrue(holder1 instanceof HeaderRecyclerAdapter.HeaderFooterViewHolder);

        RecyclerView.ViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(wrapper,
                fakeParent, wrapper.getItemViewType(0), 0);
        assertTrue(holder2 instanceof ArrayRecyclerAdapter.ViewHolder);
    }

    @Test
    public void forwardsInsertions() throws Exception {
        addHeaders(2);
        addFooters(2);
        addContent();
        observer.assertChangeOccurred(Type.INSERTED, 2, 3);
    }

    @Test
    public void forwardsChanges() throws Exception {
        addHeaders(2);
        addFooters(2);
        addContent();
        adapter.set(0, "different!");
        observer.assertChangeOccurred(Type.CHANGED, 2, 1);
    }

    @Test
    public void forwardsRemovals() throws Exception {
        addHeaders(2);
        addFooters(2);
        addContent();
        adapter.remove(0);
        observer.assertChangeOccurred(Type.REMOVED, 2, 1);
    }

    @Test
    public void forwardsMoves() throws Exception {
        addHeaders(1);
        addContent();

        adapter.notifyItemMoved(0, 1);
        observer.assertChangeOccurred(Type.MOVED, 1, 2, 1);

        adapter.notifyItemMoved(2, 1);
        observer.assertChangeOccurred(Type.MOVED, 3, 2, 1);
    }

    @Test
    public void forwardsCompleteChanges() throws Exception {
        adapter.notifyDataSetChanged();
        observer.assertChangeOccurred(Type.DATA_SET_CHANGED);
    }

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
