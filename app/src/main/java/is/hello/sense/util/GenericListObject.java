package is.hello.sense.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class GenericListObject implements ListObject {
    private ArrayList<ListItem> items = new ArrayList<>();

    public GenericListObject(final @NonNull GenericItemConverter itemConverter, final @NonNull List<Integer> set) {
        for (Integer integer : set) {
            items.add(new GenericListItem(itemConverter.getNameFor(integer), integer));
        }
    }

    @Override
    public List<? extends ListItem> getListItems() {
        return items;
    }


    public class GenericListItem implements ListItem {
        private final int value;
        private final String name;

        public GenericListItem(final String name, final int value) {
            this.value = value;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getId() {
            return value;
        }

        @Override
        public String getPreviewUrl() {
            return null;
        }
    }

    public interface GenericItemConverter {
        String getNameFor(final int value);
    }

}