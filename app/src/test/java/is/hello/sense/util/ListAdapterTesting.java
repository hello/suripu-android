package is.hello.sense.util;

import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.view.View;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class ListAdapterTesting {
    @SuppressWarnings("unchecked")
    public static <VH> VH getViewHolder(@NonNull View rowView) {
        return (VH) rowView.getTag();
    }

    public static class Observer extends DataSetObserver {
        public final List<Change> changes = new ArrayList<>();

        @Override
        public void onChanged() {
            changes.add(Change.CHANGED);
        }

        @Override
        public void onInvalidated() {
            changes.add(Change.INVALIDATED);
        }


        public boolean hasObservedChange(@NonNull Change change) {
            return changes.contains(change);
        }

        public void assertChangeOccurred(@NonNull Change change) {
            if (!hasObservedChange(change)) {
                Assert.fail("Expected change '" + change + "'");
            }
        }


        public enum Change {
            CHANGED,
            INVALIDATED,
        }
    }
}
