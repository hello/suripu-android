package is.hello.sense.util;

import android.view.View;
import android.view.ViewConfiguration;

import org.junit.Test;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SafeOnClickListenerTests extends SenseTestCase {
    @Test
    public void rateLimiting() {
        ShadowSystemClock.setCurrentTimeMillis(1000L);

        final View dummy = new View(getContext());
        final AtomicInteger counter = new AtomicInteger(0);
        final View.OnClickListener listener = new SafeOnClickListener(null, ignored -> {
            counter.incrementAndGet();
        });
        listener.onClick(dummy);
        listener.onClick(dummy);

        assertThat(counter.get(), is(equalTo(1)));

        ShadowSystemClock.sleep(100L + ViewConfiguration.getDoubleTapTimeout());

        listener.onClick(dummy);
        assertThat(counter.get(), is(equalTo(2)));
    }

    @Test
    public void stateSafety() {
        ShadowSystemClock.setCurrentTimeMillis(1000L);

        final AtomicBoolean isResumed = new AtomicBoolean(true);
        final StateSafeExecutor executor = new StateSafeExecutor(isResumed::get);

        final View dummy = new View(getContext());
        final AtomicInteger counter = new AtomicInteger(0);
        final View.OnClickListener listener = new SafeOnClickListener(executor, ignored -> {
            counter.incrementAndGet();
        });
        listener.onClick(dummy);

        assertThat(counter.get(), is(equalTo(1)));

        ShadowSystemClock.sleep(100L + ViewConfiguration.getDoubleTapTimeout());
        isResumed.set(false);
        listener.onClick(dummy);

        assertThat(counter.get(), is(equalTo(1)));

        isResumed.set(true);
        executor.executePendingForResume();

        assertThat(counter.get(), is(equalTo(2)));
    }
}
