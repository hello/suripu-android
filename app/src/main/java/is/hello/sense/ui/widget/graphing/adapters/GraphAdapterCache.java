package is.hello.sense.ui.widget.graphing.adapters;

import android.support.annotation.Nullable;
import android.util.SparseIntArray;

/**
 * Companion class for GraphAdapter that caches all of the values
 * necessary to efficiently draw the data container in an adapter.
 * Additionally, it contains several methods useful for performing
 * line graph calculations.
 */
public class GraphAdapterCache {
    public static final int NOT_FOUND = -1;

    private GraphAdapter adapter;

    private float baseMagnitude = 0f;
    private float peakMagnitude = 0f;
    private final SparseIntArray sectionCounts = new SparseIntArray();


    //region Properties

    public GraphAdapter getAdapter() {
        return adapter;
    }

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

    //endregion


    //region Calculations

    public float calculateMagnitudePercentage(float magnitude) {
        return (magnitude - peakMagnitude) / (baseMagnitude - peakMagnitude);
    }

    public float calculateSectionWidth(int hostWidth) {
        return hostWidth / getNumberSections();
    }

    public float calculateSegmentWidth(int hostWidth, int section) {
        return calculateSectionWidth(hostWidth) / getSectionCount(section);
    }

    public float calculateSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    public float calculateSegmentY(float hostHeight, int section, int position) {
        final float magnitude = adapter.getMagnitudeAt(section, position);
        final float percentage = calculateMagnitudePercentage(magnitude);
        return Math.round(hostHeight * percentage);
    }

    public int findSectionAtX(int hostWidth, float x) {
        final int limit = getNumberSections();
        if (limit == 0) {
            return NOT_FOUND;
        } else {
            return (int) Math.min(limit - 1, Math.floor(x / calculateSectionWidth(hostWidth)));
        }
    }

    public int findSegmentAtX(int hostWidth, int section, float x) {
        final int limit = getSectionCount(section);
        if (limit == 0) {
            return NOT_FOUND;
        } else {
            final float sectionMinX = calculateSectionWidth(hostWidth) * section;
            final float segmentWidth = calculateSegmentWidth(hostWidth, section);
            final float xInSection = x - sectionMinX;
            return (int) Math.min(limit - 1, xInSection / segmentWidth);
        }
    }

    //endregion


    //region Building

    public void rebuild() {
        this.sectionCounts.clear();

        if (adapter != null) {
            this.baseMagnitude = adapter.getBaseMagnitude();
            this.peakMagnitude = adapter.getPeakMagnitude();
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                final int count = adapter.getSectionPointCount(section);
                sectionCounts.append(section, count);
            }
        }
    }

    //endregion
}
