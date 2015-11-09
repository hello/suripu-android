package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.RecyclerAdapterTesting;
import is.hello.sense.util.RecyclerAdapterTesting.Observer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class HeadersRecyclerAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private final NumberAdapter adapter = new NumberAdapter(10);
    private final Observer observer = new Observer();

    public HeadersRecyclerAdapterTests() {
        adapter.registerAdapterDataObserver(observer);
    }

    @After
    public void tearDown() {
        adapter.clearHeaders();
        observer.reset();
    }


    @Test
    public void contentRendering() {
        final NumberAdapter.ViewHolder holder =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);
        assertThat(adapter.getItemCount(), is(equalTo(10)));
        assertThat(adapter.getItemViewType(0), is(equalTo(adapter.getContentItemViewType(0))));
        assertThat(holder.getContentPosition(), is(equalTo(0)));
        assertThat(holder.getAdapterPosition(), is(equalTo(0)));
        assertThat(holder.text.getText().toString(), is(equalTo("1")));
    }

    @Test
    public void headerRendering() {
        assertThat(adapter.getItemCount(), is(equalTo(10)));

        final Button header = new Button(getContext());
        adapter.addHeader(header);

        assertThat(adapter.getItemCount(), is(equalTo(11)));
        assertThat(adapter.getItemViewType(0), is(lessThan(HeadersRecyclerAdapter.VIEW_TYPE_CONTENT)));

        final HeadersRecyclerAdapter.HeaderViewHolder headerHolder =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);
        assertThat(headerHolder.container.getChildCount(), is(equalTo(1)));
        assertThat(header.getParent(), is(equalTo(headerHolder.container)));

        final NumberAdapter.ViewHolder holder =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 1);
        assertThat(holder.getContentPosition(), is(equalTo(0)));
        assertThat(holder.getAdapterPosition(), is(equalTo(1)));
        assertThat(holder.text.getText().toString(), is(equalTo("1")));
    }

    @Test
    public void headerRecycling() {
        final Button header = new Button(getContext());
        adapter.addHeader(header);

        final HeadersRecyclerAdapter.HeaderViewHolder headerHolder =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent, 0);
        assertThat(headerHolder.container.getChildCount(), is(equalTo(1)));
        assertThat(header.getParent(), is(equalTo(headerHolder.container)));

        RecyclerAdapterTesting.recycle(adapter, headerHolder);

        assertThat(headerHolder.container.getChildCount(), is(equalTo(0)));
        assertThat(header.getParent(), is(not(equalTo(headerHolder.container))));
    }

    @Test
    public void changes() {
        final Button firstHeader = new Button(getContext());
        adapter.addHeader(firstHeader);
        observer.assertChangeOccurred(Observer.Change.Type.INSERTED, 0, 1);
        assertThat(adapter.getItemCount(), is(equalTo(11)));

        final ImageView secondHeader = new ImageView(getContext());
        adapter.replaceHeader(0, secondHeader);
        observer.assertChangeOccurred(Observer.Change.Type.CHANGED, 0, 1);
        assertThat(adapter.getItemCount(), is(equalTo(11)));

        adapter.removeHeader(0);
        observer.assertChangeOccurred(Observer.Change.Type.REMOVED, 0, 1);
        assertThat(adapter.getItemCount(), is(equalTo(10)));
    }


    static class NumberAdapter extends HeadersRecyclerAdapter<NumberAdapter.ViewHolder> {
        final int count;

        public NumberAdapter(int count) {
            this.count = count;
        }

        @Override
        public int getContentItemCount() {
            return count;
        }

        @Override
        public NumberAdapter.ViewHolder onCreateContentViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindContentViewHolder(NumberAdapter.ViewHolder viewHolder, int position) {
            viewHolder.text.setText(String.format("%d", position + 1));
        }

        static class ViewHolder extends ContentViewHolder {
            final TextView text;

            ViewHolder(@NonNull TextView itemView) {
                super(itemView);

                this.text = itemView;
            }
        }
    }
}
