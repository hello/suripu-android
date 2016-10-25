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

    @SerializedName("service_name")
    private String serviceName;

    @SerializedName("enabled")
    public final boolean enabled;

    @SerializedName("target_value")
    public final ExpansionValueRange expansionRange;

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
}
