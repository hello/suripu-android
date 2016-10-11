package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class Configuration extends ApiResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("selected")
    private boolean selected;

    public Configuration(@NonNull final String id,
                         @NonNull final String name,
                         final boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "id=" + id +
                ", name=" + name +
                ", selected=" + selected +
                "}";

    }

    public static class Empty extends Configuration {

        static final String EMPTY_ID = "empty id";
        static final String EMPTY_NAME = "empty name";
        public final String title;
        public final String subtitle;
        @DrawableRes public final int iconRes;

        /**
         * @param title to be used to replace Configuration name
         * @param subtitle to be used to provide extra info
         */
        public Empty(@NonNull final String title,
                     @NonNull final String subtitle,
                     @DrawableRes final int iconRes){
            super(EMPTY_ID, EMPTY_NAME, false);
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }
    }
}
