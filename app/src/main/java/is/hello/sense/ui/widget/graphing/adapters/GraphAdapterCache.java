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

    public float getPeakMagnitude() {
        return peakMagnitude;
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

    public float calculateSectionWidth(float hostWidth) {
        return hostWidth / getNumberSections();
    }

    public float calculateSegmentWidth(float hostWidth, int section) {
        return calculateSectionWidth(hostWidth) / getSectionCount(section);
    }

    public float calculateSegmentX(float sectionWidth, float segmentWidth, int section, int position) {
        return (sectionWidth * section) + (segmentWidth * position);
    }

    public float calculateSegmentY(float hostHeight, int section, int position) {
        float magnitude = adapter.getMagnitudeAt(section, position);
        float percentage = calculateMagnitudePercentage(magnitude);
        return Math.round(hostHeight * percentage);
    }

    public int findSectionAtX(float hostWidth, float x) {
        int limit = getNumberSections();
        return (int) Math.min(limit - 1, Math.floor(x / calculateSectionWidth(hostWidth)));
    }

    public int findSegmentAtX(float hostWidth, int section, float x) {
        int limit = getSectionCount(section);
        float sectionMinX = calculateSectionWidth(hostWidth) * section;
        float segmentWidth = calculateSegmentWidth(hostWidth, section);
        float xInSection = x - sectionMinX;
        return (int) Math.min(limit - 1, xInSection / segmentWidth);
    }

    //endregion


    //region Building

    public void rebuild() {
        this.sectionCounts.clear();

        if (adapter != null) {
            this.baseMagnitude = adapter.getBaseMagnitude();
            this.peakMagnitude = adapter.getPeakMagnitude();
            for (int section = 0, sections = adapter.getSectionCount(); section < sections; section++) {
                int count = adapter.getSectionPointCount(section);
                sectionCounts.append(section, count);
            }
        }
    }

    //endregion
}
