package is.hello.sense.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import rx.Observable;

public final class SessionLogger {
    public static final String FILENAME = "Sense-Session-Log.txt";

    private static final String LOG_TAG = SessionLogger.class.getSimpleName();
    private static final int ROLLOVER = 3;

    private static boolean initialized = false;
    private static File logFile;
    private static PrintWriter printWriter;
    private static final AtomicInteger messagesWritten = new AtomicInteger();
    private static Handler handler;


    public static @NonNull String priorityToString(int priority) {
        switch (priority) {
            case Log.VERBOSE:
                return "V";
            case Log.DEBUG:
                return "D";
            case Log.INFO:
                return "I";
            case Log.WARN:
                return "W";
            case Log.ERROR:
                return "E";
            case Log.ASSERT:
                return "A";
            default:
                return "?";
        }
    }

    public static void println(int priority, @NonNull String tag, @NonNull String message) {
        if (!initialized)
            return;

        handler.post(() -> {
            try {
                int messagesWrittenSnapshot = messagesWritten.incrementAndGet();
                printWriter.printf("%s %s/%s: %s\n", DateTime.now(), priorityToString(priority), tag, message);
                if (priority == Log.ERROR || messagesWrittenSnapshot > ROLLOVER) {
                    messagesWritten.set(0);
                    printWriter.flush();
                }
            } catch (Exception e) {
                Log.wtf(SessionLogger.class.getSimpleName(), "Internal error.", e);
            }
        });
    }

    public static Observable<Void> flush() {
        if (!initialized) {
            return Observable.error(new NullPointerException());
        }

        return Observable.<Void>create(subscriber -> {
            try {
                printWriter.flush();

                subscriber.onNext(null);
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(new Rx.HandlerScheduler(handler));
    }

    public static Observable<Void> clearLog() {
        return Observable.<Void>create(subscriber -> {
            if (logFile == null) {
                subscriber.onError(new FileNotFoundException());
                return;
            }

            try {
                SessionLogger.initialized = false;

                Functions.safeClose(printWriter);
                SessionLogger.printWriter = new PrintWriter(new FileOutputStream(logFile, false));

                SessionLogger.initialized = true;

                println(Log.INFO, "Internal", "Log Cleared");
                printWriter.flush();

                subscriber.onNext(null);
                subscriber.onCompleted();
            } catch (Exception e) {
                Functions.safeClose(printWriter);
                SessionLogger.printWriter = null;
                SessionLogger.initialized = false;

                subscriber.onError(e);
            }
        }).subscribeOn(new Rx.HandlerScheduler(handler));
    }

    /**
     * Resolve the location to place the session log file.
     * <p>
     * <em>This method is not safe to call from the main thread.</em>
     *
     * @param context   The context to resolve the location through.
     * @return  The location of the session log file.
     */
    public static @NonNull String getLogFilePath(@NonNull Context context) {
        return context.getExternalCacheDir() + File.separator + FILENAME;
    }


    public static void init(@NonNull Context context) {
        if (!SessionLogger.initialized && SessionLogger.handler == null) {
            final HandlerThread workerThread = new HandlerThread("SessionLogger#handlerThread");
            workerThread.start();
            SessionLogger.handler = new Handler(workerThread.getLooper());
            handler.post(() -> {
                try {
                    final File file = new File(getLogFilePath(context));
                    SessionLogger.printWriter = new PrintWriter(new FileOutputStream(file, true));
                    SessionLogger.logFile = file;
                    SessionLogger.initialized = true;

                    println(Log.INFO, "Internal", "Session Began");
                    Log.d(LOG_TAG, "Session logger ready");

                    final IntentFilter intentFilter = new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT);
                    final Observable<Intent> logOutSignal = Rx.fromLocalBroadcast(context, intentFilter);
                    logOutSignal.subscribe(intent -> {
                                               clearLog().subscribe(ignored -> {
                                                                        Log.d(LOG_TAG, "Cleared session log for log out");
                                                                    },
                                                                    e -> {
                                                                        Log.e(LOG_TAG, "Could not clear log.", e);
                                                                    });
                                           },
                                           Functions.LOG_ERROR);
                } catch (IOException e) {
                    Logger.error(LOG_TAG, "Could not initialize session logger.", e);

                    workerThread.quitSafely();
                    SessionLogger.handler = null;

                    Functions.safeClose(printWriter);
                    SessionLogger.printWriter = null;
                    SessionLogger.initialized = false;
                }
            });
        }
    }
}
