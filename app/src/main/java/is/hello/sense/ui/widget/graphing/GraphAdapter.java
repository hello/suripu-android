package is.hello.sense.ui.widget.graphing;

public interface GraphAdapter {
    float getBaseMagnitude();
    float getPeakMagnitude();
    int getSectionCount();
    int getSectionPointCount(int section);
    float getMagnitudeAt(int section, int position);
}
