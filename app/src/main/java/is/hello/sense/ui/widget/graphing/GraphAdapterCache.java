package is.hello.sense.ui.widget.graphing;

import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Companion class for GraphAdapter that caches all of the values
 * necessary to efficiently draw the data container in an adapter.
 * Additionally, it contains several methods useful for performing
 * line graph calculations.
 */
public class GraphAdapterCache {
    private GraphAdapter adapter;

    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;
    private final SparseIntArray sectionCounts = new SparseIntArray();
    private final List<Path> sectionLinePaths = new ArrayList<>();

    //region Properties

    public void setAdapter(@Nullable GraphAdapter adapter) {
        this.adapter = adapter;
        rebuild();
    }

    public int getNumberSections() {
        return sectionCounts.size();
    }

    public int getSectionCount(int position) {
        return sectionCounts.get(position);
    }

    public @NonNull Path getSectionLinePath(int position) {
        return sectionLinePaths.get(position);
    }

    //endregion


    //region Calculations

    public float calculateMagnitudePercentage(float magnitude) {
        return (magnitude - baseMagnitude) / (peakMagnitude - baseMagnitude);
    }

    public float calculateSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    public float calculateSegmentY(float height, int section, int position) {
        float magnitude = adapter.getMagnitudeAt(section, position);
        float percentage = calculateMagnitudePercentage(magnitude);
        return Math.round(height * percentage);
    }

    //endregion


    //region Building

    private void recreateSectionPaths(int oldSize, int newSize) {
        if (newSize == 0) {
            sectionLinePaths.clear();
        } else if (newSize < oldSize) {
            int delta = oldSize - newSize;
            for (int i = 0; i < delta; i++) {
                sectionLinePaths.remove(i);
            }
        } else {
            int delta = newSize - oldSize;
            for (int i = 0; i < delta; i++) {
                sectionLinePaths.add(new Path());
            }
        }
    }

    public void rebuild() {
        int oldSize = sectionCounts.size();
        this.sectionCounts.clear();

        if (adapter != null) {
            this.baseMagnitude = adapter.getBaseMagnitude();
            this.peakMagnitude = adapter.getPeakMagnitude();
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                int count = adapter.getSectionPointCount(section);
                sectionCounts.append(section, count);
            }
        }

        int newSize = sectionCounts.size();
        recreateSectionPaths(oldSize, newSize);
    }

    //endregion
}
