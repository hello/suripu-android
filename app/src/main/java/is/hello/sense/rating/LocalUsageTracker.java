package is.hello.sense.rating;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.Months;
import org.joda.time.Weeks;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.util.Logger;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@Singleton public class LocalUsageTracker {
    public static final Days OLDEST_DAY = Days.days(60);

    private final Store store;
    private final Scheduler asyncScheduler;

    @Inject public LocalUsageTracker(@NonNull Context context) {
        this.store = new Store(context);
        this.asyncScheduler = Schedulers.io();
    }

    public DateTime today() {
        return DateTime.now().withTimeAtStartOfDay();
    }

    public void reset() {
        Logger.debug(getClass().getSimpleName(), "resetting local usage tracker");

        SQLiteDatabase database = store.getWritableDatabase();
        database.delete(Store.TABLE_USAGE, null, null);
    }

    public void resetAsync() {
        Scheduler.Worker worker = asyncScheduler.createWorker();
        worker.schedule(() -> {
            try {
                reset();
            } catch (RuntimeException e) {
                Logger.error(getClass().getSimpleName(), "Failure in resetAsync", e);
            } finally {
                worker.unsubscribe();
            }
        });
    }

    public void reset(@NonNull Identifier identifier) {
        Logger.debug(getClass().getSimpleName(), "resetting count for " + identifier);

        SQLiteDatabase database = store.getWritableDatabase();

        String where = Store.COLUMN_ID + " = ?";
        String[] whereArgs = {
                Integer.toString(identifier.value),
        };

        ContentValues values = new ContentValues(1);
        values.put(Store.COLUMN_COUNT, 0);
        database.update(Store.TABLE_USAGE,
                        values,
                        where,
                        whereArgs);
    }

    public void increment(@NonNull Identifier identifier) {
        Logger.debug(getClass().getSimpleName(), "incrementing count for " + identifier);

        SQLiteDatabase database = store.getWritableDatabase();

        long today = today().getMillis();
        String[] columns = {Store.COLUMN_COUNT};
        String where = Store.COLUMN_ID + " = ?" +
                "AND " + Store.COLUMN_TIMESTAMP + " = ?";
        String[] whereArgs = {
            Integer.toString(identifier.value),
            Long.toString(today),
        };

        Cursor cursor = database.query(Store.TABLE_USAGE,
                                       columns,
                                       where,
                                       whereArgs,
                                       null, null, null);
        try {
            if (cursor.moveToNext()) {
                ContentValues values = new ContentValues(1);
                values.put(Store.COLUMN_COUNT, cursor.getInt(0) + 1);
                database.update(Store.TABLE_USAGE,
                                values,
                                where,
                                whereArgs);
            } else {
                ContentValues values = new ContentValues(3);
                values.put(Store.COLUMN_TIMESTAMP, today);
                values.put(Store.COLUMN_ID, identifier.value);
                values.put(Store.COLUMN_COUNT, 1);
                database.insert(Store.TABLE_USAGE, null, values);
            }
        } finally {
            cursor.close();
        }
    }

    public void incrementAsync(@NonNull Identifier identifier) {
        Scheduler.Worker worker = asyncScheduler.createWorker();
        worker.schedule(() -> {
            try {
                increment(identifier);
            } catch (RuntimeException e) {
                Logger.error(getClass().getSimpleName(), "Failure in incrementAsync", e);
            } finally {
                worker.unsubscribe();
            }
        });
    }

    public int usageWithin(@NonNull Identifier identifier,
                           @NonNull Interval interval) {
        SQLiteDatabase database = store.getReadableDatabase();
        String[] columns = {"SUM(count)"};
        String selection = "id = ? AND timestamp >= ? AND timestamp <= ?";
        String[] selectionArgs = {
            Integer.toString(identifier.value),
            Long.toString(interval.getStartMillis()),
            Long.toString(interval.getEndMillis()),
        };
        Cursor query = database.query(Store.TABLE_USAGE,
                                      columns,
                                      selection,
                                      selectionArgs,
                                      null, null, null);
        try {
            if (query.moveToNext()) {
                return query.getInt(0);
            } else {
                return 0;
            }
        } finally {
            query.close();
        }
    }

    public void deleteOldUsageStats() {
        Logger.debug(getClass().getSimpleName(), "Deleting old usage statistics");

        long limit = today().minus(OLDEST_DAY).getMillis();
        SQLiteDatabase database = store.getWritableDatabase();
        int purgeCount = database.delete(Store.TABLE_USAGE,
                                         "timestamp < ?",
                                         new String[]{Long.toString(limit)});
        Logger.debug(getClass().getSimpleName(), "Purged " + purgeCount + " old usage statistics");
    }

    public void deleteOldUsageStatsAsync() {
        Scheduler.Worker worker = asyncScheduler.createWorker();
        worker.schedule(() -> {
            try {
                deleteOldUsageStats();
            } catch (RuntimeException e) {
                Logger.error(getClass().getSimpleName(), "Failure in deleteOldUsageStatsAsync", e);
            } finally {
                worker.unsubscribe();
            }
        });
    }

    /**
     * Aggregates the user's usage patterns for the last month
     * to determine if they should be shown a rating prompt.
     * <p>
     * This method is very expensive, and should not be called
     * on the main thread.
     *
     * @return true if the user's usage patterns match the
     *         profile for a rating prompt; false otherwise.
     */
    public boolean isUsageAcceptableForRatingPrompt() {
        DateTime today = today();
        Interval lastWeek = new Interval(Weeks.ONE, today);
        int appLaunches = usageWithin(Identifier.APP_LAUNCHED, lastWeek);

        Interval lastMonth = new Interval(Months.ONE, today);
        int systemAlertsShown = usageWithin(Identifier.SYSTEM_ALERT_SHOWN, lastMonth);
        int timelinesShown = usageWithin(Identifier.TIMELINE_SHOWN_WITH_DATA, lastMonth);

        Interval last60Days = new Interval(Days.days(60), today);
        int skipReviewPrompt = usageWithin(Identifier.SKIP_REVIEW_PROMPT, last60Days);

        Logger.info(getClass().getSimpleName(),
                    "App launches (7 days): " + appLaunches +
                    "\nSystem alerts shown (1 month): " + systemAlertsShown +
                    "\nTimelines shown (1 month): " + timelinesShown +
                    "\nSkip review prompt (2 months): " + skipReviewPrompt);

        return (appLaunches > 4 &&
                systemAlertsShown == 0 &&
                timelinesShown > 10 &&
                skipReviewPrompt == 0);
    }


    public enum Identifier {
        SYSTEM_ALERT_SHOWN(0),
        APP_LAUNCHED(1),
        TIMELINE_SHOWN_WITH_DATA(2),
        SKIP_REVIEW_PROMPT(3);

        /**
         * The value used within the usage database.
         * Must never change for a given DB version.
         */
        public final int value;

        Identifier(int value) {
            this.value = value;
        }
    }

    static class Store extends SQLiteOpenHelper {
        static final String NAME = "UsageDatabase";
        static final int VERSION = 1;

        static final String TABLE_USAGE = "usage";
        static final String COLUMN_ID = "id";
        static final String COLUMN_TIMESTAMP = "timestamp";
        static final String COLUMN_COUNT = "count";

        Store(@NonNull Context context) {
            super(context, NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USAGE + "(" +
                               COLUMN_ID + " INTEGER, " +
                               COLUMN_TIMESTAMP + " INTEGER, " +
                               COLUMN_COUNT + " INTEGER" +
                               ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Nothing for now
        }
    }
}
