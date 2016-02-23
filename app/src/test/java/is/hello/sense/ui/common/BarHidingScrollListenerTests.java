package is.hello.sense.ui.common;

import android.view.MotionEvent;

import org.joda.time.DateTimeUtils;
import org.junit.Test;

import is.hello.sense.R;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.ui.widget.ExtendedScrollView;
import is.hello.sense.util.LambdaVar;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BarHidingScrollListenerTests extends SenseTestCase {
    private static BarHidingScrollListener.Listener createMockListener() {
        return mock(BarHidingScrollListener.Listener.class);
    }

    private static ExtendedScrollView createMockScrollView(int scrollY) {
        final ExtendedScrollView scrollView = mock(ExtendedScrollView.class);
        doReturn(scrollY)
                .when(scrollView)
                .getScrollY();
        return scrollView;
    }

    private float getBarHeight() {
        return getResources().getDimension(R.dimen.action_bar_height);
    }

    private static MotionEvent obtainActionDownEvent() {
        final long time = DateTimeUtils.currentTimeMillis();
        return MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0f, 0f, 0);
    }

    private static MotionEvent obtainActionUpEvent() {
        final long time = DateTimeUtils.currentTimeMillis();
        return MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, 0f, 0f, 0);
    }

    @Test
    public void tracking() {
        final BarHidingScrollListener.Listener listener = createMockListener();
        final ExtendedScrollView scrollView = createMockScrollView(0);

        final BarHidingScrollListener scrollListener = new BarHidingScrollListener(listener,
                                                                                   getBarHeight(),
                                                                                   0f);

        scrollListener.onScrollChanged(0, 1,
                                       0, 0);
        scrollListener.onScrollChanged(0, 2,
                                       0, 1);
        scrollListener.onScrollChanged(0, 3,
                                       0, 2);

        verify(listener, never()).onHideAmountChanged(any(float.class));

        final MotionEvent downEvent = obtainActionDownEvent();
        scrollListener.onTouch(scrollView, downEvent);
        downEvent.recycle();

        scrollListener.onScrollChanged(0, 0,
                                       0, 3);
        verify(listener, times(1)).onHideAmountChanged(any(float.class));

        final MotionEvent upEvent = obtainActionUpEvent();
        scrollListener.onTouch(scrollView, upEvent);
        upEvent.recycle();

        scrollListener.onScrollChanged(0, 1,
                                       0, 0);
        verify(listener, times(1)).onHideAmountChanged(any(float.class));
    }

    @Test
    public void snapUp() {
        final LambdaVar<Float> lastHideAmount = LambdaVar.of(0f);
        final BarHidingScrollListener.Listener listener = lastHideAmount::set;

        final float barHeight = getBarHeight();
        final ExtendedScrollView scrollView = createMockScrollView(0);
        final BarHidingScrollListener scrollListener = new BarHidingScrollListener(listener,
                                                                                   barHeight,
                                                                                   0f);

        final MotionEvent downEvent = obtainActionDownEvent();
        scrollListener.onTouch(scrollView, downEvent);
        downEvent.recycle();

        scrollListener.onScrollChanged(0, Math.round(barHeight / 2),
                                       0, 0);
        assertThat(lastHideAmount.get(), is(equalTo(0.5f)));

        final MotionEvent upEvent = obtainActionUpEvent();
        scrollListener.onTouch(scrollView, upEvent);
        upEvent.recycle();

        assertThat(lastHideAmount.get(), is(equalTo(1f)));
    }

    @Test
    public void snapDown() {
        final LambdaVar<Float> lastHideAmount = LambdaVar.of(0f);
        final BarHidingScrollListener.Listener listener = lastHideAmount::set;

        final float barHeight = getBarHeight();
        final ExtendedScrollView scrollView = createMockScrollView(Math.round(barHeight / 2f));
        final BarHidingScrollListener scrollListener = new BarHidingScrollListener(listener,
                                                                                   barHeight,
                                                                                   0.5f);

        final MotionEvent downEvent = obtainActionDownEvent();
        scrollListener.onTouch(scrollView, downEvent);
        downEvent.recycle();

        scrollListener.onScrollChanged(0, Math.round(barHeight * 0.25f),
                                       0, Math.round(barHeight / 2f));
        assertThat(lastHideAmount.get(), is(equalTo(0.5f)));

        final MotionEvent upEvent = obtainActionUpEvent();
        scrollListener.onTouch(scrollView, upEvent);
        upEvent.recycle();

        assertThat(lastHideAmount.get(), is(equalTo(0f)));
    }
}
