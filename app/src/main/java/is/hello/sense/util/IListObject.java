package is.hello.sense.util;


import java.io.Serializable;
import java.util.List;

public interface IListObject<T extends IListObject.IListItem> extends Serializable {
    List<T> getListItems();


    interface IListItem extends Serializable {
        String getName();

        int getId();

        String getPreviewUrl();
    }

}