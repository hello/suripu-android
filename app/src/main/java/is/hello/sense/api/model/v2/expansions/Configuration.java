package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import is.hello.sense.api.model.ApiResponse;

public class Configuration extends ApiResponse {
    @SerializedName("id")
    private final String id;

    @SerializedName("name")
    private final String name;

    @SerializedName("selected")
    private boolean selected;

    @SerializedName("capabilities")
    private List<Capability> capabilities;

    /**
     * set to true if configuration should be treated as empty object
     * in place of null checks
     */
    @Expose(serialize = false)
    private boolean isEmpty;

    public Configuration(@NonNull final String id,
                         @NonNull final String name,
                         final boolean selected,
                         final boolean isEmpty) {
        this.id = id;
        this.name = name;
        this.selected = selected;
        this.capabilities = Collections.singletonList(Capability.UNKNOWN);
        this.isEmpty = isEmpty;
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

    /**
     * until server supports this it will default to list containing single
     * {@link Capability#UNKNOWN}
     */
    @NonNull
    public List<Capability> getCapabilities(){
        if(capabilities == null){
          return Collections.singletonList(Capability.UNKNOWN);
        }
        return capabilities;
    }

    public boolean isEmpty(){
        return isEmpty;
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
            super(EMPTY_ID, EMPTY_NAME, false, true);
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }
    }
}
