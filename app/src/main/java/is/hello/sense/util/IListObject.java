package is.hello.sense.util;


import java.io.Serializable;
import java.util.List;

public interface IListObject extends Serializable {
    List<? extends IListItem> getListItems();


    interface IListItem extends Serializable {
        String getName();

        int getId();

        String getPreviewUrl();
    }

}