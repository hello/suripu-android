package is.hello.sense.ui.widget.graphing;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class GridRecyclerTests extends SenseTestCase
        implements GridRecycler.Adapter<FrameLayout, View> {
    private GridRecycler<FrameLayout, View> recycler;
    private int createRowCalls = 0;
    private int createCellCalls = 0;
    private int rowRecycledCalls = 0;
    private int cellRecycledCalls = 0;

    //region Lifecycle

    @Before
    public void setUp() {
        this.recycler = new GridRecycler<>(this);
    }

    @After
    public void tearDown() {
        this.createRowCalls = 0;
        this.createCellCalls = 0;
        this.rowRecycledCalls = 0;
        this.cellRecycledCalls = 0;
    }

    @Override
    public FrameLayout onCreateRowView() {
        this.createRowCalls++;
        return new FrameLayout(getContext());
    }

    @Override
    public View onCreateCellView() {
        this.createCellCalls++;
        return new View(getContext());
    }

    @Override
    public void onRowRecycled(@NonNull FrameLayout rowView) {
        this.rowRecycledCalls++;
    }

    @Override
    public void onCellRecycled(@NonNull View cellView) {
        this.cellRecycledCalls++;
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
        assertThat(rowRecycledCalls, is(equalTo(3)));

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
        assertThat(cellRecycledCalls, is(equalTo(7)));

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
        assertThat(rowRecycledCalls, is(equalTo(1)));

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
        assertThat(cellRecycledCalls, is(equalTo(2)));

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

    @Test
    public void emptyScrap() {
        recycler.prime(2, 7);
        assertThat(createRowCalls, is(equalTo(2)));
        assertThat(createCellCalls, is(equalTo(2 * 7)));

        recycler.emptyScrap();
        this.createRowCalls = 0;
        this.createCellCalls = 0;

        final FrameLayout row = recycler.dequeueRowView();
        assertThat(row, is(notNullValue()));

        final View cell = recycler.dequeueCellView();
        assertThat(cell, is(notNullValue()));

        assertThat(createRowCalls, is(equalTo(1)));
        assertThat(createCellCalls, is(equalTo(1)));
    }
}
