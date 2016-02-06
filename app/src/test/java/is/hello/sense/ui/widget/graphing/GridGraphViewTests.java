package is.hello.sense.ui.widget.graphing;

import android.view.View;
import android.widget.FrameLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class GridGraphViewTests extends SenseTestCase {
    @Test
    public void requestPopulateCoalesces() {
        final GridGraphView view = spy(new GridGraphView(getContext()));

        final List<Runnable> pending = new ArrayList<>();
        final Answer postAnswer = invocation -> {
            final Runnable task = invocation.getArgumentAt(0, Runnable.class);
            pending.add(task);
            return null;
        };
        doAnswer(postAnswer)
                .when(view)
                .post(any(Runnable.class));

        final Answer removeCallbacksAnswer = invocation -> {
            final Runnable task = invocation.getArgumentAt(0, Runnable.class);
            pending.remove(task);
            return null;
        };
        doAnswer(removeCallbacksAnswer)
                .when(view)
                .removeCallbacks(any(Runnable.class));

        view.requestPopulate();
        view.requestPopulate();
        view.requestPopulate();

        assertThat(pending.size(), is(equalTo(1)));
    }

    public static class RecyclerTests extends SenseTestCase {
        private GridGraphView.Recycler<FrameLayout, View> recycler;
        private int createRowCalls = 0;
        private int createCellCalls = 0;

        //region Lifecycle

        @Before
        public void setUp() {
            this.recycler = new GridGraphView.Recycler<>(this::createRow,
                                                         this::createCell);
        }

        @After
        public void tearDown() {
            this.createRowCalls = 0;
            this.createCellCalls = 0;
        }

        public FrameLayout createRow() {
            this.createRowCalls++;
            return new FrameLayout(getContext());
        }

        public View createCell() {
            this.createCellCalls++;
            return new View(getContext());
        }

        //endregion


        @Test
        public void setMaxRowScrapCount() {
            recycler.setMaxRowScrapCount(2);

            final FrameLayout firstRow = recycler.dequeueRowView();
            assertThat(firstRow, is(notNullValue()));

            final FrameLayout secondRow = recycler.dequeueRowView();
            assertThat(secondRow, is(notNullValue()));

            final FrameLayout thirdRow = recycler.dequeueRowView();
            assertThat(thirdRow, is(notNullValue()));

            assertThat(createRowCalls, is(equalTo(3)));

            recycler.recycleRow(firstRow);
            recycler.recycleRow(secondRow);
            recycler.recycleRow(thirdRow);

            assertThat(recycler.dequeueRowView(), is(notNullValue()));
            assertThat(recycler.dequeueRowView(), is(notNullValue()));
            assertThat(recycler.dequeueRowView(), is(notNullValue()));

            assertThat(createRowCalls, is(equalTo(4)));
        }

        @Test
        public void setMaxCellScrapCount() {
            recycler.setMaxCellScrapCount(3);

            final FrameLayout row = recycler.dequeueRowView();
            assertThat(row, is(notNullValue()));

            for (int i = 0; i < 7; i++) {
                row.addView(recycler.dequeueCellView());
            }
            assertThat(createCellCalls, is(equalTo(7)));

            recycler.recycleUnusedRowCells(row, 0);

            for (int i = 0; i < 4; i++) {
                row.addView(recycler.dequeueCellView());
            }
            assertThat(createCellCalls, is(equalTo(8)));
        }

        @Test
        public void createsNewRows() {
            final FrameLayout row = recycler.dequeueRowView();
            assertThat(row, is(notNullValue()));
            assertThat(createRowCalls, is(equalTo(1)));
        }

        @Test
        public void createsNewCells() {
            final View cell = recycler.dequeueCellView();
            assertThat(cell, is(notNullValue()));
            assertThat(createCellCalls, is(equalTo(1)));
        }

        @Test
        public void recycleRows() {
            final FrameLayout firstRow = recycler.dequeueRowView();
            assertThat(firstRow, is(notNullValue()));
            assertThat(createRowCalls, is(equalTo(1)));

            recycler.recycleRow(firstRow);

            final FrameLayout secondRow = recycler.dequeueRowView();
            assertThat(secondRow, is(notNullValue()));
            assertThat(createRowCalls, is(equalTo(1)));
            assertThat(secondRow, is(sameInstance(firstRow)));

            final FrameLayout thirdRow = recycler.dequeueRowView();
            assertThat(thirdRow, is(notNullValue()));
            assertThat(createRowCalls, is(equalTo(2)));
            assertThat(thirdRow, is(not(sameInstance(firstRow))));
            assertThat(thirdRow, is(not(sameInstance(secondRow))));
        }

        @Test
        public void recycleCells() {
            final FrameLayout row = recycler.dequeueRowView();
            assertThat(row, is(notNullValue()));
            assertThat(createRowCalls, is(equalTo(1)));

            for (int i = 0; i < 7; i++) {
                row.addView(recycler.dequeueCellView());
            }
            assertThat(createCellCalls, is(equalTo(7)));
            assertThat(row.getChildCount(), is(equalTo(7)));

            recycler.recycleUnusedRowCells(row, 5);
            assertThat(row.getChildCount(), is(equalTo(5)));

            for (int i = 0; i < 2; i++) {
                row.addView(recycler.dequeueCellView());
            }
            assertThat(createCellCalls, is(equalTo(7)));
            assertThat(row.getChildCount(), is(equalTo(7)));
        }

        @Test
        public void prime() {
            recycler.prime(2, 7);
            assertThat(createRowCalls, is(equalTo(2)));
            assertThat(createCellCalls, is(equalTo(2 * 7)));

            for (int r = 0; r < 2; r++) {
                final FrameLayout row = recycler.dequeueRowView();
                assertThat(row, is(notNullValue()));
                assertThat(row.getChildCount(), is(equalTo(7)));
            }

            assertThat(createRowCalls, is(equalTo(2)));
            assertThat(createCellCalls, is(equalTo(2 * 7)));
        }
    }
}
