package is.hello.sense.util;


import java.io.Serializable;
import java.util.List;

public interface ListObject extends Serializable {
    List<? extends ListItem> getListOptions();

    // true = checkbox, false = radio
    boolean multipleOptions();

    interface ListItem extends Serializable {
        String getName();

        int getId();

        String getPreviewUrl();
    }

}