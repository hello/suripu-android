package is.hello.sense.flows.sensordetails.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.interactors.Interactor;
import is.hello.sense.interactors.PreferencesInteractor;

@Singleton
public class SensorLabelInteractor extends Interactor {
    private static final int ELAPSED_TIME_FOR_UPDATE_MS = 300000; // 5 minute

    private final Helper weekHelper = new Helper();
    private final Helper dayHelper = new Helper();

    @Inject
    PreferencesInteractor preferencesInteractor;

    @NonNull
    public String[] getWeekLabels() {
        if (weekHelper.shouldUpdate()) {
            weekHelper.update(generateWeekLabels(Calendar.getInstance()));
        }
        return weekHelper.labels;
    }

    @NonNull
    @VisibleForTesting
    String[] generateWeekLabels(@NonNull final Calendar calendar) {
        final String[] labels = new String[7];
        final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        for (int i = 0; i < labels.length; i++) {
            calendar.add(Calendar.DATE, 1);
            final String day = dateFormat.format(calendar.getTime());
            labels[i] = day.toUpperCase();
        }
        return labels;
    }

    @NonNull
    public String[] getDayLabels() {
        if (dayHelper.shouldUpdate()) {
            dayHelper.update(generateDayLabels(Calendar.getInstance()));
        }
        return dayHelper.labels;

    }

    @NonNull
    @VisibleForTesting
    String[] generateDayLabels(@NonNull final Calendar calendar) {
        final String[] labels = new String[7];
        final SimpleDateFormat dateFormat;
        final int minuteDiff;
        if (this.preferencesInteractor.getUse24Time()) {
            dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            final int unRoundedMins = calendar.get(Calendar.MINUTE) % 15;
            calendar.add(Calendar.MINUTE, unRoundedMins < 8 ? -unRoundedMins : (15 - unRoundedMins));
            calendar.add(Calendar.MINUTE, 15);
            minuteDiff = -25;
        } else {
            dateFormat = new SimpleDateFormat("ha", Locale.getDefault());
            final int unRoundedMins = calendar.get(Calendar.MINUTE) % 30;
            calendar.add(Calendar.MINUTE, 30 - unRoundedMins);
            minuteDiff = -30;
        }
        calendar.add(Calendar.HOUR, -2);

        for (int i = 6; i >= 0; i--) {
            final String day = dateFormat.format(calendar.getTime());
            labels[i] = day.toUpperCase();
            calendar.add(Calendar.HOUR, -3);
            calendar.add(Calendar.MINUTE, minuteDiff);
        }
        return labels;
    }

    private class Helper {
        private String[] labels;
        private long lastTimeUpdatedMillis = 0;

        private boolean shouldUpdate() {
            return labels == null || labels.length == 0 ||
                    System.currentTimeMillis() - lastTimeUpdatedMillis > ELAPSED_TIME_FOR_UPDATE_MS;
        }

        private void update(@NonNull final String[] labels) {
            lastTimeUpdatedMillis = System.currentTimeMillis();
            this.labels = labels;
        }
    }
}
