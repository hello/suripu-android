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

import javax.inject.Inject;

public class LocalUsageTracker {
    public static final Days OLDEST_DAY = Days.days(31);

    private final Store store;

    @Inject public LocalUsageTracker(@NonNull Context context) {
        this.store = new Store(context);
    }

    public DateTime today() {
        return DateTime.now().withTimeAtStartOfDay();
    }

    public void reset() {
        SQLiteDatabase database = store.getWritableDatabase();
        database.delete(Store.TABLE_USAGE, null, null);
    }

    public void increment(@NonNull Identifier identifier) {
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
        long limit = today().minus(OLDEST_DAY).getMillis();
        SQLiteDatabase database = store.getWritableDatabase();
        database.delete(Store.TABLE_USAGE,
                        "timestamp < ?",
                        new String[] { Long.toString(limit) });
    }


    public enum Identifier {
        SYSTEM_ALERT_SHOWN(0),
        APP_LAUNCHED(1),
        TIMELINE_SHOWN_WITH_DATA(2);

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
