package is.hello.sense.ui.widget.graphing.adapters;

import android.support.annotation.NonNull;

public interface GraphAdapter {
    float getBaseMagnitude();
    float getPeakMagnitude();
    int getSectionCount();
    int getSectionPointCount(int section);
    float getMagnitudeAt(int section, int position);

    void registerObserver(@NonNull ChangeObserver observer);
    void unregisterObserver(@NonNull ChangeObserver observer);

    public interface ChangeObserver {
        void onGraphAdapterChanged();
    }
}
