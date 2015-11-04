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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FooterRecyclerAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final TestAdapter adapter = new TestAdapter();
    private final FooterRecyclerAdapter wrapper = new FooterRecyclerAdapter(adapter);
    private final RecyclerAdapterTesting.Observer observer = new RecyclerAdapterTesting.Observer();

    public FooterRecyclerAdapterTests() {
        wrapper.registerAdapterDataObserver(observer);
    }

    @After
    public void tearDown() {
        wrapper.footers.clear();

        adapter.setOnItemClickedListener(null);
        adapter.clear();

        observer.reset();
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
        addFooters(2);
        addContent();
        assertThat(wrapper.getItemCount(), is(equalTo(5)));

        assertThat(wrapper.getItemId(0), is(equalTo(0L)));
        assertThat(wrapper.getItemId(1), is(equalTo(0L)));
        assertThat(wrapper.getItemId(2), is(equalTo(0L)));
        assertThat(wrapper.getItemId(3), is(equalTo(Long.MAX_VALUE)));
        assertThat(wrapper.getItemId(4), is(equalTo(Long.MAX_VALUE - 1L)));

        assertThat(wrapper.getItemId(0), is(equalTo(0L)));
        assertThat(wrapper.getItemId(1), is(equalTo(0L)));
        assertThat(wrapper.getItemId(2), is(equalTo(0L)));
        assertThat(wrapper.getItemViewType(3), is(equalTo(FooterRecyclerAdapter.VIEW_ID_FOOTER)));
        assertThat(wrapper.getItemViewType(4), is(equalTo(FooterRecyclerAdapter.VIEW_ID_FOOTER)));
    }

    @Test
    public void footers() throws Exception {
        addContent();
        assertThat(wrapper.getItemCount(), is(equalTo(3)));

        addFooters(1);
        assertThat(wrapper.getItemCount(), is(equalTo(4)));
        assertThat(wrapper.getItemId(0), is(equalTo(0L)));
        assertThat(wrapper.getItemViewType(3), is(equalTo(FooterRecyclerAdapter.VIEW_ID_FOOTER)));
        observer.assertChangeOccurred(Type.INSERTED, 3, 1);

        RecyclerView.ViewHolder holder1 = RecyclerAdapterTesting.createAndBindView(wrapper,
                fakeParent, wrapper.getItemViewType(3), 3);
        assertThat(holder1, is(instanceOf(FooterRecyclerAdapter.FooterViewHolder.class)));

        RecyclerView.ViewHolder holder2 = RecyclerAdapterTesting.createAndBindView(wrapper,
                fakeParent, wrapper.getItemViewType(0), 0);
        assertThat(holder2, is(instanceOf(ArrayRecyclerAdapter.ViewHolder.class)));
    }

    @Test
    public void forwardsInsertions() throws Exception {
        addFooters(2);
        addContent();
        observer.assertChangeOccurred(Type.INSERTED, 0, 3);
    }

    @Test
    public void forwardsChanges() throws Exception {
        addFooters(2);
        addContent();
        adapter.set(0, "different!");
        observer.assertChangeOccurred(Type.CHANGED, 0, 1);
    }

    @Test
    public void forwardsRemovals() throws Exception {
        addFooters(2);
        addContent();
        adapter.remove(0);
        observer.assertChangeOccurred(Type.REMOVED, 0, 1);
    }

    @Test
    public void forwardsMoves() throws Exception {
        addContent();

        adapter.notifyItemMoved(0, 1);
        observer.assertChangeOccurred(Type.MOVED, 0, 1, 1);

        adapter.notifyItemMoved(2, 1);
        observer.assertChangeOccurred(Type.MOVED, 2, 1, 1);
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
