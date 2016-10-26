package is.hello.sense.api.model.v2.expansions;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.gson.Exclude;
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

    @SerializedName("service_name")
    private String serviceName;

    @SerializedName("enabled")
    public final boolean enabled;

    @SerializedName("target_value")
    public final ExpansionValueRange expansionRange;

    @Exclude
    private String displayValue = "empty value";
    @Exclude
    private Drawable displayIcon = null;

    public ExpansionAlarm(final long id,
                          @NonNull final Category category,
                          @NonNull final String serviceName,
                          final boolean enabled,
                          @NonNull final ExpansionValueRange range){
        this.id = id;
        this.category = category;
        this.serviceName = serviceName;
        this.enabled = enabled;
        this.expansionRange = range;
    }

    public ExpansionAlarm(@NonNull final Expansion expansion) {
        this(expansion.getId(),
             expansion.getCategory(),
             expansion.getServiceName(),
             true,
             expansion.getValueRange());
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

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(@NonNull final String displayValue){
        this.displayValue = displayValue;
    }

    public Drawable getDisplayIcon() {
        return displayIcon;
    }

    public void setDisplayIcon(@NonNull final Drawable displayIcon) {
        this.displayIcon = displayIcon;
    }
}
