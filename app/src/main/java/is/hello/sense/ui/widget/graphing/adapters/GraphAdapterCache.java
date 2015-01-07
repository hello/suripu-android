package is.hello.sense.ui.widget.graphing.adapters;

import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;

import java.security.InvalidParameterException;
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
    private final @Nullable List<Path> sectionLinePaths;


    public GraphAdapterCache(@NonNull Type type) {
        switch (type) {
            case PLAIN: {
                this.sectionLinePaths = null;
                break;
            }

            case STYLEABLE: {
                this.sectionLinePaths = new ArrayList<>();
                break;
            }

            default: {
                throw new InvalidParameterException();
            }
        }
    }


    //region Properties

    public GraphAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(@Nullable GraphAdapter adapter) {
        this.adapter = adapter;
        rebuild();
    }

    public float getPeakMagnitude() {
        return peakMagnitude;
    }

    public int getNumberSections() {
        return sectionCounts.size();
    }

    public int getSectionCount(int position) {
        return sectionCounts.get(position);
    }

    public @NonNull Path getSectionLinePath(int position) {
        if (sectionLinePaths == null) {
            throw new IllegalStateException("getSectionLinePath is only available with STYLEABLE adapter caches");
        }

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
        if (sectionLinePaths == null) {
            throw new IllegalStateException("recreateSectionPaths is only available with STYLEABLE adapter caches");
        }

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

        if (sectionLinePaths != null) {
            int newSize = sectionCounts.size();
            recreateSectionPaths(oldSize, newSize);
        }
    }

    //endregion


    public static enum Type {
        PLAIN,
        STYLEABLE,
    }
}
