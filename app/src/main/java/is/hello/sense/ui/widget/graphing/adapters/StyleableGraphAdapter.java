package is.hello.sense.ui.widget.graphing.adapters;

import android.support.annotation.NonNull;

public interface StyleableGraphAdapter extends GraphAdapter {
    int getSectionLineColor(int section);
    int getSectionTextColor(int section);
    @NonNull String getSectionHeader(int section);
    @NonNull String getSectionFooter(int section);
}
