package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import is.hello.buruberi.util.Either;
import is.hello.sense.api.model.v2.Timeline;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;
import rx.schedulers.Schedulers;

import static is.hello.sense.AssertExtensions.assertNoThrow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ZoomedOutTimelineInteractorTests extends InjectionTestCase {
    private final LocalDate startTime = LocalDate.now();

    @Inject
    ZoomedOutTimelineInteractor presenter;

    @Before
    public void initialize() throws Exception {
        presenter.clearDataViews();
        presenter.setFirstDate(startTime);
        presenter.setUpdateScheduler(Schedulers.computation());
    }

    @Test
    public void lowMemoryResponse() throws Exception {
        presenter.getCachedTimelines().put(startTime, new Timeline());
        assertEquals(1, presenter.getCachedTimelines().size());

        presenter.onTrimMemory(Interactor.BASE_TRIM_LEVEL);
        assertEquals(0, presenter.getCachedTimelines().size());
    }


    @Test
    public void getDateAt() throws Exception {
        assertEquals(startTime.minusDays(5), presenter.getDateAt(5));
    }

    @Test
    public void getDatePosition() throws Exception {
        assertEquals(4, presenter.getDatePosition(startTime.minusDays(5)));
    }


    @Test
    public void caching() throws Exception {
        assertNoThrow(() -> presenter.cacheTimeline(startTime, null));

        Timeline toCache = new Timeline();
        presenter.cacheTimeline(startTime, toCache);
        assertNotNull(presenter.getCachedTimeline(startTime));
    }

    @Test
    public void retrieveAndCacheTimelineFresh() throws Exception {
        LocalDate date = LocalDate.now();
        Timeline timeline = Sync.last(presenter.retrieveAndCacheTimeline(date));
        assertNotNull(timeline);

        Timeline cached = presenter.getCachedTimeline(date);
        assertNotNull(cached);
        assertSame(timeline, cached);
    }

    @Test
    public void retrieveAndCacheTimelineExisting() throws Exception {
        LocalDate date = LocalDate.now();
        Timeline toCache = new Timeline();
        presenter.cacheTimeline(date, toCache);

        assertSame(toCache, presenter.getCachedTimeline(date));
    }

    @Test
    public void retrieveTimelines() throws Exception {
        LocalDate date = LocalDate.now();
        Batch firstBatch = Batch.create(date, 3).addTo(presenter);

        presenter.retrieveTimelines();
        firstBatch.await();
        firstBatch.assertAllUpdated();
        firstBatch.removeFrom(presenter);


        Batch secondBatch = Batch.create(date, 3).addTo(presenter);

        presenter.retrieveTimelines();
        secondBatch.await();
        secondBatch.assertAllUpdated();

        firstBatch.assertNoneUpdated();
    }


    static class Batch {
        final CountDownLatch countDownLatch;
        final TestView[] testViews;

        static Batch create(@NonNull LocalDate LocalDate, int size) {
            TestView[] testViews = new TestView[size];
            CountDownLatch countDownLatch = new CountDownLatch(size);
            for (int i = 0; i < size; i++) {
                testViews[i] = new TestView(LocalDate.minusDays(i), countDownLatch);
            }

            return new Batch(countDownLatch, testViews);
        }

        Batch(@NonNull CountDownLatch countDownLatch, @NonNull TestView[] testViews) {
            this.countDownLatch = countDownLatch;
            this.testViews = testViews;
        }


        Batch addTo(@NonNull ZoomedOutTimelineInteractor presenter) {
            for (TestView testView : testViews) {
                presenter.addDataView(testView);
            }

            return this;
        }

        Batch removeFrom(@NonNull ZoomedOutTimelineInteractor presenter) {
            for (TestView testView : testViews) {
                testView.clearResult();
                presenter.removeDataView(testView);
            }
            return this;
        }

        void await() throws InterruptedException {
            countDownLatch.await();
        }

        void assertAllUpdated() {
            for (TestView testView : testViews) {
                Either<Timeline, Throwable> result = testView.getResult();
                assertNotNull(result);
                assertTrue(result.isLeft());
                assertNotNull(result.getLeft());
            }
        }

        void assertNoneUpdated() {
            for (TestView testView : testViews) {
                assertNull(testView.getResult());
            }

        }
    }

    static class TestView implements ZoomedOutTimelineInteractor.DataView {
        private final LocalDate LocalDate;
        private final CountDownLatch countDownLatch;

        private boolean wantsUpdates = true;
        private @Nullable Either<Timeline, Throwable> result = null;

        TestView(@NonNull LocalDate LocalDate,
                 @NonNull CountDownLatch countDownLatch) {
            this.LocalDate = LocalDate;
            this.countDownLatch = countDownLatch;
        }


        @Override
        public LocalDate getDate() {
            return LocalDate;
        }

        @Override
        public boolean wantsUpdates() {
            return wantsUpdates;
        }


        @Override
        public void onUpdateAvailable(@NonNull Timeline timeline) {
            this.result = Either.left(timeline);
            countDownLatch.countDown();
        }

        @Override
        public void onUpdateFailed(Throwable e) {
            this.result = Either.right(e);
            countDownLatch.countDown();
        }

        @Override
        public void cancelAnimation(boolean showLoading) {

        }

        void clearResult() {
            this.result = null;
        }

        @Nullable Either<Timeline, Throwable> getResult() {
            return result;
        }
    }
}
