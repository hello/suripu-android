package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

/**
 * Provide to {@link is.hello.sense.api.model.Alarm} to specify which
 * expansions are enabled
 */
public class ExpansionAlarm extends ApiResponse {
    @SerializedName("id")
    private final long id;

    @SerializedName("category")
    private Category category;

    @SerializedName("enabled")
    public final boolean enabled;

    public ExpansionAlarm(final long id, @NonNull final Category category, final boolean enabled){
        this.id = id;
        this.category = category;
        this.enabled = enabled;
    }

    public ExpansionAlarm(@NonNull final Expansion expansion) {
        this(expansion.getId(), expansion.getCategory(), true);
    }

    public long getId(){
        return id;
    }

    public Category getCategory() {
        if(category == null){
            return Category.UNKNOWN;
        } else {
            return category;
        }
    }

    public void setCategory(@NonNull final Category category) {
        this.category = category;
    }

    public boolean isEnabled(){
        return enabled;
    }
}
